package com.resjk.restaurant.service;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.resjk.restaurant.model.*;
import java.util.*;
import java.util.stream.Collectors;
import javax.persistence.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class OrderQueryService {
  private final EntityManager em;
  public OrderQueryService(EntityManager em) { this.em = em; }

  public static class OrderRow {
    @JsonUnwrapped public final Orders order;
    public final Payment payment;
    public OrderRow(Orders order, Payment payment) { this.order = order; this.payment = payment; }
  }
  public static class PaymentRow {
    @JsonUnwrapped public final Payment payment;
    public final String orderNumber;
    public final java.time.LocalDate orderDate;
    public PaymentRow(Payment p, Orders o) { payment = p; orderNumber = o.getOrderNumber(); orderDate = o.getOrderDate(); }
  }

  public PageResult<OrderRow> orders(DatePageFilter filter, SessionUser user, String customer) {
    String cno = "customer".equals(user.role) ? user.id : customer;
    String where = " where o.orderDate >= :startDate and o.orderDate <= :endDate" + (cno == null ? "" : " and o.cno = :cno");
    TypedQuery<Orders> query = em.createQuery("select o from Orders o" + where + " order by o.orderTime desc, o.ono desc", Orders.class);
    TypedQuery<Long> count = em.createQuery("select count(o) from Orders o" + where, Long.class);
    bindOrderDates(query, filter, cno); bindOrderDates(count, filter, cno);
    List<Orders> orders = query.setFirstResult(filter.page * filter.size).setMaxResults(filter.size).getResultList();
    Map<String, Payment> payments = new HashMap<>();
    if (!orders.isEmpty()) em.createQuery("select p from Payment p where p.ono in :ids", Payment.class)
        .setParameter("ids", orders.stream().map(Orders::getOno).collect(Collectors.toList())).getResultList().forEach(p -> payments.put(p.getOno(), p));
    return new PageResult<>(orders.stream().map(o -> new OrderRow(o, payments.get(o.getOno()))).collect(Collectors.toList()), count.getSingleResult(), filter);
  }

  public PageResult<PaymentRow> payments(DatePageFilter filter, SessionUser user) {
    String cno = "customer".equals(user.role) ? user.id : null;
    String where = " from Payment p, Orders o where p.ono = o.ono and p.paidAt >= :start and p.paidAt < :end" + (cno == null ? "" : " and o.cno = :cno");
    TypedQuery<Object[]> query = em.createQuery("select p, o" + where + " order by p.paidAt desc, p.paymentNo desc", Object[].class);
    TypedQuery<Long> count = em.createQuery("select count(p)" + where, Long.class);
    bind(query, filter, cno); bind(count, filter, cno);
    List<PaymentRow> rows = query.setFirstResult(filter.page * filter.size).setMaxResults(filter.size).getResultList().stream()
        .map(r -> new PaymentRow((Payment) r[0], (Orders) r[1])).collect(Collectors.toList());
    return new PageResult<>(rows, count.getSingleResult(), filter);
  }

  private void bind(Query query, DatePageFilter f, String cno) {
    query.setParameter("start", f.from()).setParameter("end", f.until());
    if (cno != null) query.setParameter("cno", cno);
  }

  private void bindOrderDates(Query query, DatePageFilter f, String cno) {
    query.setParameter("startDate", f.startDate).setParameter("endDate", f.endDate);
    if (cno != null) query.setParameter("cno", cno);
  }
}
