package com.resjk.restaurant.service;

import com.resjk.restaurant.model.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import javax.persistence.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ReservationService {
  private static final Set<String> METHODS = new HashSet<>(Arrays.asList("DEMO_WECHAT", "DEMO_ALIPAY", "DEMO_CASH"));
  private static final Set<LocalTime> CUSTOMER_SLOT_STARTS = new HashSet<>(Arrays.asList(
      LocalTime.of(11, 0), LocalTime.of(13, 30), LocalTime.of(17, 0), LocalTime.of(19, 30)));
  private final EntityManager em;
  private final Clock clock;
  private final boolean demoPaymentsEnabled;

  public ReservationService(EntityManager em, Clock clock, @Value("${app.demo-payments-enabled:true}") boolean enabled) {
    this.em = em; this.clock = clock; this.demoPaymentsEnabled = enabled;
  }

  public static class CreateRequest {
    public String customerName;
    public String customerPhone;
    public String tno;
    public Integer partySize;
    public LocalDateTime reservedAt;
    public String depositMethod;
  }

  public static class DepositRefund {
    public final String reservationNo;
    public final BigDecimal amount;
    public final String status;
    public final LocalDateTime refundedAt;
    public DepositRefund(Reservation reservation) {
      reservationNo = reservation.getReservationNo(); amount = reservation.getDepositAmount();
      status = reservation.getDepositStatus(); refundedAt = reservation.getDepositRefundedAt();
    }
  }

  public static class AvailabilityBlock {
    public final String tno;
    public final LocalDateTime reservedAt;
    public final LocalDateTime reservedUntil;
    public AvailabilityBlock(String tno, LocalDateTime reservedAt, LocalDateTime reservedUntil) {
      this.tno = tno; this.reservedAt = reservedAt; this.reservedUntil = reservedUntil;
    }
  }

  @Transactional(readOnly = true)
  public List<Reservation> list(SessionUser user) {
    if ("customer".equals(user.role)) {
      return em.createQuery("select r from Reservation r where r.cno=:cno order by r.reservedAt desc, r.createdAt desc", Reservation.class)
          .setParameter("cno", user.id).setMaxResults(100).getResultList();
    }
    requireStaff(user);
    return em.createQuery("select r from Reservation r order by r.reservedAt desc, r.createdAt desc", Reservation.class).setMaxResults(100).getResultList();
  }

  @Transactional(readOnly = true)
  public List<AvailabilityBlock> availability(LocalDate date, SessionUser user) {
    if (date == null) bad("请选择预约日期");
    LocalDate today = LocalDate.now(clock);
    if (date.isBefore(today) || date.isAfter(today.plusYears(1))) bad("仅可查询今天至未来一年内的预约时段");
    if (!Arrays.asList("customer", "staff", "admin").contains(user.role)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权查看预约时段");
    LocalDateTime start = date.atStartOfDay();
    LocalDateTime end = start.plusDays(1);
    return em.createQuery("select r from Reservation r where r.status in ('RESERVED','SEATED') and r.reservedAt<:end and r.reservedUntil>:start order by r.tno,r.reservedAt", Reservation.class)
        .setParameter("start", start).setParameter("end", end).getResultList().stream()
        .map(r -> new AvailabilityBlock(r.getTno(), r.getReservedAt(), r.getReservedUntil()))
        .collect(java.util.stream.Collectors.toList());
  }

  @Transactional
  public void syncTodayTableStatuses() {
    LocalDateTime start = LocalDate.now(clock).atStartOfDay();
    LocalDateTime end = start.plusDays(1);
    Set<String> reservedToday = new HashSet<>(em.createQuery("select distinct r.tno from Reservation r where r.status='RESERVED' and r.reservedAt<:end and r.reservedUntil>:start", String.class)
        .setParameter("start", start).setParameter("end", end).getResultList());
    List<DiningTable> idleTables = em.createQuery("select t from DiningTable t where t.tstatus in ('空闲','已预订')", DiningTable.class).getResultList();
    for (DiningTable table : idleTables) table.setTstatus(reservedToday.contains(table.getTno()) ? "已预订" : "空闲");
  }

  @Transactional
  public Reservation create(CreateRequest request, SessionUser user) {
    if (!demoPaymentsEnabled) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "模拟押金支付已关闭，暂时不能确认预约");
    boolean customerSelfService = "customer".equals(user.role);
    Customer customer = customerSelfService ? em.find(Customer.class, user.id) : null;
    if (customerSelfService && customer == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "顾客账号不存在");
    if (!customerSelfService) requireStaff(user);
    String name = customerSelfService ? trim(customer.getCname()) : trim(request.customerName);
    String phone = customerSelfService ? trim(customer.getCphone()) : trim(request.customerPhone);
    if (name == null || name.length() > 30) bad("请输入 1 至 30 个字的预约人姓名");
    if (phone == null || !phone.matches("[0-9+\\- ]{6,20}")) bad(customerSelfService ? "请先在个人信息中完善联系电话" : "请输入 6 至 20 位有效联系电话");
    if (request.partySize == null || request.partySize < 1 || request.partySize > 100) bad("用餐人数应为 1 至 100 人");
    if (request.reservedAt == null || request.reservedAt.isBefore(LocalDateTime.now(clock).minusMinutes(5)) || request.reservedAt.isAfter(LocalDateTime.now(clock).plusYears(1))) {
      bad("预约时间应在当前时间至未来一年内");
    }
    if (!METHODS.contains(request.depositMethod)) bad("请选择有效的模拟押金支付方式");
    if (customerSelfService && "DEMO_CASH".equals(request.depositMethod)) bad("顾客在线预约请选择微信或支付宝模拟支付");
    if (customerSelfService && !CUSTOMER_SLOT_STARTS.contains(request.reservedAt.toLocalTime())) bad("请选择系统提供的预约时间段");
    DiningTable table = em.find(DiningTable.class, request.tno, LockModeType.PESSIMISTIC_WRITE);
    if (table == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "所选餐桌不存在");
    int seats = table.getSeats() == null ? 0 : table.getSeats();
    if (seats < request.partySize) throw new ResponseStatusException(HttpStatus.CONFLICT, "用餐人数超过餐桌座位数");

    LocalDateTime reservedUntil = request.reservedAt.plusHours(2);
    long overlapping = em.createQuery("select count(r) from Reservation r where r.tno=:tno and r.status in ('RESERVED','SEATED') and r.reservedAt<:reservedUntil and r.reservedUntil>:reservedAt", Long.class)
        .setParameter("tno", table.getTno()).setParameter("reservedAt", request.reservedAt)
        .setParameter("reservedUntil", reservedUntil).getSingleResult();
    if (overlapping > 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "所选餐桌在该时间段已被预约，请选择其他时段");

    LocalDateTime now = LocalDateTime.now(clock);
    Reservation reservation = new Reservation();
    reservation.setReservationNo("R" + UUID.randomUUID().toString().replace("-", ""));
    reservation.setCustomerName(name); reservation.setCustomerPhone(phone);
    reservation.setCno(customerSelfService ? user.id : findCustomerNumber(phone)); reservation.setTno(table.getTno()); reservation.setEno(employeeNumber(user));
    reservation.setPartySize(request.partySize.shortValue()); reservation.setReservationSource(customerSelfService ? "ONLINE" : "PHONE"); reservation.setReservedAt(request.reservedAt);
    reservation.setReservedUntil(reservedUntil); reservation.setArrivalDeadline(request.reservedAt.plusMinutes(30));
    reservation.setDepositAmount(calculateDeposit(seats)); reservation.setDepositMethod(request.depositMethod);
    reservation.setDepositStatus("PAID"); reservation.setDepositPaidAt(now);
    reservation.setStatus("RESERVED"); reservation.setCreatedAt(now);
    em.persist(reservation);
    if ("空闲".equals(table.getTstatus()) && request.reservedAt.toLocalDate().equals(LocalDate.now(clock))) table.setTstatus("已预订");
    em.flush();
    return reservation;
  }

  @Scheduled(fixedDelayString = "${app.reservation-expiry-interval-ms:60000}", initialDelayString = "${app.reservation-expiry-initial-delay-ms:60000}")
  @Transactional
  public int expireNoShows() {
    LocalDateTime now = LocalDateTime.now(clock);
    List<Object[]> due = em.createQuery("select r.reservationNo,r.tno from Reservation r where r.status='RESERVED' and r.arrivalDeadline<=:now", Object[].class)
        .setParameter("now", now).getResultList();
    int expired = 0;
    for (Object[] row : due) {
      String reservationNo = (String) row[0];
      String tno = (String) row[1];
      DiningTable table = em.find(DiningTable.class, tno, LockModeType.PESSIMISTIC_WRITE);
      Reservation reservation = em.find(Reservation.class, reservationNo, LockModeType.PESSIMISTIC_WRITE);
      if (reservation == null || !"RESERVED".equals(reservation.getStatus()) || reservation.getArrivalDeadline().isAfter(now)) continue;
      forfeitNoShow(reservation, table, now);
      expired++;
    }
    return expired;
  }

  @Transactional
  public Reservation markNoShow(String reservationNo, SessionUser user) {
    requireStaff(user);
    Reservation reservation = em.find(Reservation.class, reservationNo);
    if (reservation == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "预约记录不存在");
    DiningTable table = em.find(DiningTable.class, reservation.getTno(), LockModeType.PESSIMISTIC_WRITE);
    em.lock(reservation, LockModeType.PESSIMISTIC_WRITE);
    if (!"RESERVED".equals(reservation.getStatus())) throw new ResponseStatusException(HttpStatus.CONFLICT, "当前预约状态不能标记为未到店");
    LocalDateTime now = LocalDateTime.now(clock);
    if (reservation.getReservedAt().isAfter(now)) bad("预约时间尚未开始，不能标记顾客未到店");
    forfeitNoShow(reservation, table, now);
    em.flush();
    return reservation;
  }

  @Transactional
  public Reservation cancel(String reservationNo, SessionUser user) {
    requireStaff(user);
    Reservation reservation = em.find(Reservation.class, reservationNo);
    if (reservation == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "预约记录不存在");
    DiningTable table = em.find(DiningTable.class, reservation.getTno(), LockModeType.PESSIMISTIC_WRITE);
    em.lock(reservation, LockModeType.PESSIMISTIC_WRITE);
    if (!"RESERVED".equals(reservation.getStatus())) throw new ResponseStatusException(HttpStatus.CONFLICT, "当前预约状态不能取消");
    LocalDateTime now = LocalDateTime.now(clock);
    reservation.setStatus("CANCELLED");
    if ("PAID".equals(reservation.getDepositStatus())) {
      reservation.setDepositStatus("REFUNDED"); reservation.setDepositRefundedAt(now);
    }
    releaseTableAfterReservation(table, reservation.getReservationNo());
    em.flush();
    return reservation;
  }

  public void attachToOrder(String tno, String cno, String ono, String reservationNo, SessionUser user) {
    Reservation reservation;
    if (reservationNo != null && !reservationNo.trim().isEmpty()) {
      reservation = em.find(Reservation.class, reservationNo, LockModeType.PESSIMISTIC_WRITE);
      if (reservation == null || !"RESERVED".equals(reservation.getStatus())) throw new ResponseStatusException(HttpStatus.CONFLICT, "所选预约已失效，请刷新后重试");
      if (!tno.equals(reservation.getTno())) throw new ResponseStatusException(HttpStatus.CONFLICT, "订单餐桌与预约餐桌不一致");
    } else {
      List<Reservation> matches = em.createQuery("select r from Reservation r where r.tno=:tno and r.status='RESERVED' order by r.reservedAt", Reservation.class)
          .setParameter("tno", tno).setLockMode(LockModeType.PESSIMISTIC_WRITE).setMaxResults(1).getResultList();
      if (matches.isEmpty()) throw new ResponseStatusException(HttpStatus.CONFLICT, "餐桌标记为已预订，但没有有效预约记录");
      reservation = matches.get(0);
    }
    if ("customer".equals(user.role) && (reservation.getCno() == null || !reservation.getCno().equals(cno))) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "请联系员工核对电话预约并办理入座");
    }
    if (reservation.getCno() != null && !reservation.getCno().equals(cno)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "订单顾客与预约顾客不一致");
    }
    if (reservation.getCno() == null) reservation.setCno(cno);
    reservation.setOno(ono); reservation.setStatus("SEATED");
  }

  public DepositRefund refundForOrder(String ono) {
    List<Reservation> matches = em.createQuery("select r from Reservation r where r.ono=:ono", Reservation.class)
        .setParameter("ono", ono).setLockMode(LockModeType.PESSIMISTIC_WRITE).setMaxResults(1).getResultList();
    if (matches.isEmpty()) return null;
    Reservation reservation = matches.get(0);
    if ("PAID".equals(reservation.getDepositStatus())) {
      reservation.setDepositStatus("REFUNDED"); reservation.setDepositRefundedAt(LocalDateTime.now(clock));
    }
    reservation.setStatus("COMPLETED");
    return new DepositRefund(reservation);
  }

  public boolean reopenAfterOrderDeletion(String ono) {
    List<Reservation> matches = em.createQuery("select r from Reservation r where r.ono=:ono and r.status='SEATED'", Reservation.class)
        .setParameter("ono", ono).setLockMode(LockModeType.PESSIMISTIC_WRITE).setMaxResults(1).getResultList();
    if (matches.isEmpty()) return false;
    Reservation reservation = matches.get(0);
    reservation.setOno(null); reservation.setStatus("RESERVED");
    return true;
  }

  public void detachOrderReference(String ono) {
    List<Reservation> matches = em.createQuery("select r from Reservation r where r.ono=:ono", Reservation.class)
        .setParameter("ono", ono).setLockMode(LockModeType.PESSIMISTIC_WRITE).setMaxResults(1).getResultList();
    if (!matches.isEmpty()) matches.get(0).setOno(null);
  }

  public void moveSeatedReservation(String ono, String targetTno) {
    List<Reservation> matches = em.createQuery("select r from Reservation r where r.ono=:ono and r.status='SEATED'", Reservation.class)
        .setParameter("ono", ono).setLockMode(LockModeType.PESSIMISTIC_WRITE).setMaxResults(1).getResultList();
    if (!matches.isEmpty()) matches.get(0).setTno(targetTno);
  }

  @Transactional(readOnly = true)
  public void validateManualTableStatusChange(String current, String desired, String tno) {
    if ("已预订".equals(desired) && !"已预订".equals(current)) bad("请在预约管理中收取押金并确认预约");
    if ("已预订".equals(current) && !"已预订".equals(desired)) {
      long active = em.createQuery("select count(r) from Reservation r where r.tno=:tno and r.status='RESERVED'", Long.class)
          .setParameter("tno", tno).getSingleResult();
      if (active > 0) bad("请在预约管理中取消预约并退还押金");
    }
  }

  public static BigDecimal calculateDeposit(int seats) {
    int steps = Math.max(0, (Math.max(seats, 2) - 1) / 2);
    return BigDecimal.valueOf(20L + steps * 10L).setScale(2);
  }

  private void forfeitNoShow(Reservation reservation, DiningTable table, LocalDateTime now) {
    reservation.setStatus("NO_SHOW");
    reservation.setDepositStatus("FORFEITED");
    reservation.setDepositForfeitedAt(now);
    releaseTableAfterReservation(table, reservation.getReservationNo());
  }

  private void releaseTableAfterReservation(DiningTable table, String reservationNo) {
    if (table == null || !"已预订".equals(table.getTstatus())) return;
    LocalDateTime start = LocalDate.now(clock).atStartOfDay();
    LocalDateTime end = start.plusDays(1);
    long remaining = em.createQuery("select count(r) from Reservation r where r.tno=:tno and r.status='RESERVED' and r.reservationNo<>:reservationNo and r.reservedAt<:end and r.reservedUntil>:start", Long.class)
        .setParameter("tno", table.getTno()).setParameter("reservationNo", reservationNo)
        .setParameter("start", start).setParameter("end", end).getSingleResult();
    if (remaining == 0) table.setTstatus("空闲");
  }

  private String findCustomerNumber(String phone) {
    return em.createQuery("select c.cno from Customer c where c.cphone=:phone order by c.cno", String.class)
        .setParameter("phone", phone).setMaxResults(1).getResultStream().findFirst().orElse(null);
  }
  private String employeeNumber(SessionUser user) {
    if ("staff".equals(user.role)) return user.id;
    return em.createQuery("select e.eno from Employee e order by e.eno", String.class).setMaxResults(1).getResultStream()
        .findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "暂无员工，不能登记预约"));
  }
  private void requireStaff(SessionUser user) {
    if (!"staff".equals(user.role) && !"admin".equals(user.role)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只有员工可以管理电话预约");
  }
  private String trim(String value) { return value == null || value.trim().isEmpty() ? null : value.trim(); }
  private void bad(String message) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
}
