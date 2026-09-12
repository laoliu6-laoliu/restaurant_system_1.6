package com.resjk.restaurant.service;

import com.resjk.restaurant.model.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import javax.persistence.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CheckoutService {
  private final EntityManager em;
  private final boolean demoPaymentsEnabled;
  private final java.time.Clock clock;
  private final DailyOrderSequence sequence;
  private final ReservationService reservations;

  public CheckoutService(EntityManager em, @Value("${app.demo-payments-enabled:true}") boolean enabled, java.time.Clock clock, DailyOrderSequence sequence, ReservationService reservations) {
    this.em = em;
    this.demoPaymentsEnabled = enabled;
    this.clock = clock; this.sequence = sequence; this.reservations = reservations;
  }

  public boolean isDemoPaymentsEnabled() { return demoPaymentsEnabled; }

  public static class CheckoutRequest {
    public String cno;
    public String tno;
    public String reservationNo;
    public List<Line> items;
  }

  public static class Line {
    public String mno;
    public Integer dishCount;
    public String remark;
  }

  public static class PaymentResult {
    @com.fasterxml.jackson.annotation.JsonUnwrapped public final Payment payment;
    public final ReservationService.DepositRefund depositRefund;
    public PaymentResult(Payment payment, ReservationService.DepositRefund depositRefund) {
      this.payment = payment; this.depositRefund = depositRefund;
    }
  }

  public static class TableTransferRequest {
    public LocalDate orderDate;
    public String orderNumber;
    public Integer partySize;
    public String targetTno;
  }

  public static class TableTransferResult {
    public final String ono;
    public final LocalDate orderDate;
    public final String orderNumber;
    public final String oldTno;
    public final String newTno;
    public final Integer targetSeats;
    public final Integer partySize;
    public final LocalDateTime transferredAt;
    public TableTransferResult(Orders order, String oldTno, DiningTable target, int partySize, LocalDateTime transferredAt) {
      this.ono = order.getOno(); this.orderDate = order.getOrderDate(); this.orderNumber = order.getOrderNumber();
      this.oldTno = oldTno; this.newTno = target.getTno(); this.targetSeats = target.getSeats() == null ? null : target.getSeats().intValue();
      this.partySize = partySize; this.transferredAt = transferredAt;
    }
  }

  private ResponseStatusException bad(String message) {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
  }

  private <T> T require(Class<T> type, String id, boolean lock) {
    T entity = id == null ? null : (lock ? em.find(type, id, LockModeType.PESSIMISTIC_WRITE) : em.find(type, id));
    if (entity == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "所选记录不存在，请刷新重试");
    return entity;
  }

  private String newId(String prefix) { return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 8); }

  @Transactional
  public Orders checkout(CheckoutRequest request, SessionUser user) {
    String cno = "customer".equals(user.role) ? user.id : request.cno;
    require(Customer.class, cno, false);
    if (request.items == null || request.items.isEmpty() || request.items.size() > 100) throw bad("请选择 1 至 100 道菜品");
    DiningTable table = require(DiningTable.class, request.tno, true);
    if (!"空闲".equals(table.getTstatus()) && !"已预订".equals(table.getTstatus())) throw new ResponseStatusException(HttpStatus.CONFLICT, "餐桌已被占用，请重新选择");
    boolean reservedTable = "已预订".equals(table.getTstatus());
    String ono = newId("O");
    Set<String> seen = new HashSet<>();
    for (Line line : request.items) {
      if (line == null || line.mno == null || !seen.add(line.mno)) throw bad("菜品不能为空或重复，请合并相同菜品");
      if (line.dishCount == null || line.dishCount < 1 || line.dishCount > 32767) throw bad("菜品数量必须为正整数");
      if (line.remark != null && line.remark.length() > 100) throw bad("备注不能超过 100 个字");
    }
    // Consistent lock order avoids deadlocks when customers buy the same dishes.
    request.items.sort(Comparator.comparing(line -> line.mno));
    BigDecimal total = BigDecimal.ZERO;
    List<OrderDetail> details = new ArrayList<>();
    for (Line line : request.items) {
      MenuItem dish = require(MenuItem.class, line.mno, true);
      if (dish.getStock() == null || dish.getStock() < line.dishCount) throw new ResponseStatusException(HttpStatus.CONFLICT, dish.getMname() + "库存不足");
      if (dish.getMprice() == null || dish.getMprice().signum() <= 0) throw bad("菜品价格无效，请联系员工");
      BigDecimal amount = dish.getMprice().multiply(BigDecimal.valueOf(line.dishCount));
      total = total.add(amount);
      OrderDetail detail = new OrderDetail();
      detail.setDno(newId("D")); detail.setOno(ono); detail.setMno(line.mno);
      detail.setDishCount(line.dishCount.shortValue()); detail.setAmount(amount); detail.setRemark(line.remark);
      details.add(detail);
      dish.setStock(dish.getStock() - line.dishCount);
    }
    if (total.compareTo(new BigDecimal("999999.99")) > 0) throw bad("订单金额超出上限，请分单");
    String eno = "staff".equals(user.role) ? user.id : em.createQuery("select e.eno from Employee e order by e.eno", String.class)
        .setMaxResults(1).getResultStream().findFirst().orElseThrow(() -> bad("暂无员工，请联系管理员"));
    LocalDateTime orderTime = LocalDateTime.now(clock);
    int dailyNumber = sequence.next(orderTime.toLocalDate());
    Orders order = new Orders();
    order.setOrderDate(orderTime.toLocalDate()); order.setDailyNumber(dailyNumber);
    order.setOno(ono); order.setCno(cno); order.setEno(eno); order.setTno(request.tno);
    order.setOrderTime(orderTime); order.setTotalAmount(total); order.setPaymentStatus("UNPAID"); order.setOrderStatus("ACTIVE");
    em.persist(order);
    if (request.reservationNo != null || reservedTable) reservations.attachToOrder(table.getTno(), cno, ono, request.reservationNo, user);
    details.forEach(em::persist);
    table.setTstatus("使用中");
    em.flush();
    return order;
  }

  @Transactional
  public TableTransferResult transferTable(TableTransferRequest request, SessionUser user) {
    if (!"staff".equals(user.role) && !"admin".equals(user.role)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "换桌请联系员工办理");
    }
    if (request == null || request.orderDate == null) throw bad("请选择订单日期");
    String visibleNumber = request.orderNumber == null ? "" : request.orderNumber.trim();
    if (!visibleNumber.matches("\\d{3}")) throw bad("订单编号必须是 001 至 999 的三位编号");
    int dailyNumber = Integer.parseInt(visibleNumber);
    if (dailyNumber < 1 || dailyNumber > 999) throw bad("订单编号必须是 001 至 999 的三位编号");
    if (request.partySize == null || request.partySize < 1 || request.partySize > 100) throw bad("实际到店人数应为 1 至 100 人");
    String targetTno = request.targetTno == null ? "" : request.targetTno.trim();
    if (targetTno.isEmpty()) throw bad("请选择新餐桌");

    List<Orders> matches = em.createQuery("select o from Orders o where o.orderDate=:orderDate and o.dailyNumber=:dailyNumber", Orders.class)
        .setParameter("orderDate", request.orderDate).setParameter("dailyNumber", dailyNumber)
        .setLockMode(LockModeType.PESSIMISTIC_WRITE).setMaxResults(1).getResultList();
    if (matches.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "所选日期没有该订单编号");
    Orders order = matches.get(0);
    if ("ENDED".equals(order.getOrderStatus())) throw new ResponseStatusException(HttpStatus.CONFLICT, "已结束订单不能换桌");
    if (targetTno.equals(order.getTno())) throw bad("新餐桌不能与原餐桌相同");

    List<String> tableNumbers = new ArrayList<>(Arrays.asList(order.getTno(), targetTno));
    Collections.sort(tableNumbers);
    Map<String, DiningTable> lockedTables = new HashMap<>();
    for (String tno : tableNumbers) {
      DiningTable table = em.find(DiningTable.class, tno, LockModeType.PESSIMISTIC_WRITE);
      if (table == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "餐桌 " + tno + " 不存在");
      lockedTables.put(tno, table);
    }
    DiningTable source = lockedTables.get(order.getTno());
    DiningTable target = lockedTables.get(targetTno);
    int sourceSeats = source.getSeats() == null ? 0 : source.getSeats();
    int targetSeats = target.getSeats() == null ? 0 : target.getSeats();
    if (!"使用中".equals(source.getTstatus())) throw new ResponseStatusException(HttpStatus.CONFLICT, "原餐桌当前不是使用中状态，请刷新后重试");
    if (request.partySize <= sourceSeats) throw bad("当前餐桌可容纳该人数，无需换桌");
    if (!"空闲".equals(target.getTstatus())) throw new ResponseStatusException(HttpStatus.CONFLICT, "新餐桌已被占用，请重新选择");
    if (targetSeats < request.partySize) throw new ResponseStatusException(HttpStatus.CONFLICT, "新餐桌座位数不足，不能容纳实际到店人数");

    String oldTno = source.getTno();
    order.setTno(target.getTno());
    reservations.moveSeatedReservation(order.getOno(), target.getTno());
    target.setTstatus("使用中");
    long remaining = em.createQuery("select count(o) from Orders o where o.tno=:tno and o.orderStatus='ACTIVE' and o.ono<>:ono", Long.class)
        .setParameter("tno", oldTno).setParameter("ono", order.getOno()).getSingleResult();
    if (remaining == 0) source.setTstatus("待清理");
    LocalDateTime transferredAt = LocalDateTime.now(clock);
    em.flush();
    return new TableTransferResult(order, oldTno, target, request.partySize, transferredAt);
  }

  @Transactional
  public PaymentResult pay(String ono, String method, SessionUser user) {
    if (!demoPaymentsEnabled) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "模拟支付已关闭，暂未配置实际收款渠道");
    if (!Arrays.asList("DEMO_WECHAT", "DEMO_ALIPAY", "DEMO_CASH").contains(method)) throw bad("请选择有效的模拟支付方式");
    Orders order = require(Orders.class, ono, true);
    user.checkCustomer(order.getCno());
    if ("DEMO_CASH".equals(method) && "customer".equals(user.role)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "现金收款请联系员工");
    List<Payment> existing = em.createQuery("select p from Payment p where p.ono = :ono", Payment.class).setParameter("ono", ono).getResultList();
    if (!existing.isEmpty()) return new PaymentResult(existing.get(0), reservations.refundForOrder(ono));
    if ("ENDED".equals(order.getOrderStatus())) throw new ResponseStatusException(HttpStatus.CONFLICT, "已结束订单不能支付");
    if (!"UNPAID".equals(order.getPaymentStatus())) throw new ResponseStatusException(HttpStatus.CONFLICT, "订单状态不允许支付");
    List<OrderDetail> details = em.createQuery("select d from OrderDetail d where d.ono = :ono", OrderDetail.class).setParameter("ono", ono).getResultList();
    if (details.isEmpty()) throw bad("订单没有菜品明细，不能支付");
    if (details.stream().anyMatch(d -> d.getAmount() == null || d.getAmount().signum() <= 0 || d.getDishCount() == null || d.getDishCount() <= 0)) throw bad("订单明细无效，请联系员工核对");
    BigDecimal amount = details.stream().map(OrderDetail::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    if (amount.signum() <= 0 || amount.compareTo(order.getTotalAmount()) != 0) throw bad("订单金额与明细不一致，请联系员工核对");
    Payment payment = new Payment();
    payment.setPaymentNo("P" + UUID.randomUUID().toString().replace("-", ""));
    payment.setOno(ono); payment.setAmount(order.getTotalAmount()); payment.setMethod(method);
    payment.setStatus("SIMULATED_SUCCESS"); payment.setPaidAt(LocalDateTime.now(clock));
    em.persist(payment);
    order.setPaymentStatus("PAID");
    em.flush();
    return new PaymentResult(payment, reservations.refundForOrder(ono));
  }

  @Transactional
  public Orders endOrder(String ono, SessionUser user) {
    requireStaffOrAdmin(user, "结束订单请联系员工或管理员");
    Orders order = require(Orders.class, ono, true);
    if ("ENDED".equals(order.getOrderStatus())) return order;
    DiningTable table = require(DiningTable.class, order.getTno(), true);
    order.setOrderStatus("ENDED");
    order.setEndedAt(LocalDateTime.now(clock));
    long remaining = em.createQuery("select count(o) from Orders o where o.tno=:tno and o.orderStatus='ACTIVE' and o.ono<>:ono", Long.class)
        .setParameter("tno", table.getTno()).setParameter("ono", order.getOno()).getSingleResult();
    if (remaining == 0 && "使用中".equals(table.getTstatus())) table.setTstatus("待清理");
    em.flush();
    return order;
  }

  @Transactional
  public void deleteEnded(String ono, SessionUser user) {
    requireStaffOrAdmin(user, "删除订单请联系员工或管理员");
    Orders order = require(Orders.class, ono, true);
    if (!"ENDED".equals(order.getOrderStatus())) throw new ResponseStatusException(HttpStatus.CONFLICT, "只能删除已结束订单");
    // Lock table before stock, matching checkout's lock order.
    DiningTable table = require(DiningTable.class, order.getTno(), true);
    List<OrderDetail> details = em.createQuery("select d from OrderDetail d where d.ono = :ono order by d.mno", OrderDetail.class).setParameter("ono", ono).getResultList();
    for (OrderDetail detail : details) {
      if ("UNPAID".equals(order.getPaymentStatus())) {
        MenuItem dish = require(MenuItem.class, detail.getMno(), true);
        dish.setStock((dish.getStock() == null ? 0 : dish.getStock()) + detail.getDishCount());
      }
      em.remove(detail);
    }
    boolean reservationReopened = "UNPAID".equals(order.getPaymentStatus()) && reservations.reopenAfterOrderDeletion(ono);
    if (!reservationReopened) reservations.detachOrderReference(ono);
    em.createQuery("select p from Payment p where p.ono=:ono", Payment.class).setParameter("ono", ono).getResultList().forEach(em::remove);
    em.remove(order); em.flush();
    if (reservationReopened) { table.setTstatus("已预订"); return; }
    long remaining = em.createQuery("select count(o) from Orders o where o.tno=:tno and o.orderStatus='ACTIVE'", Long.class).setParameter("tno", table.getTno()).getSingleResult();
    if (remaining == 0 && "使用中".equals(table.getTstatus())) table.setTstatus("待清理");
  }

  private void requireStaffOrAdmin(SessionUser user, String message) {
    if (!"staff".equals(user.role) && !"admin".equals(user.role)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, message);
  }
}
