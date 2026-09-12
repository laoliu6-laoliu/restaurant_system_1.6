package com.resjk.restaurant.controller;

import java.math.BigDecimal;
import com.resjk.restaurant.service.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {
  private final EntityManager em;

  private final java.time.Clock clock;
  private final OrderQueryService queries;
  public ReportController(EntityManager em, java.time.Clock clock, OrderQueryService queries) {
    this.em = em; this.clock = clock; this.queries = queries;
  }

  @GetMapping("/order-detail-view")
  public PageResult<Map<String, Object>> orderDetailView(
      @RequestParam(required = false) String startDate, @RequestParam(required = false) String endDate,
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
    DatePageFilter filter = new DatePageFilter(startDate, endDate, page, size, clock);
    List<Object[]> rows = em.createNativeQuery(
        "SELECT v.ono, v.order_time, v.cno, v.cname, v.eno, v.ename, v.tno, v.mno, v.mname, v.mtype, v.dish_count, v.dish_price, v.amount, v.remark, o.daily_number "
        + "FROM v_order_detail_info v JOIN orders o ON o.ono=v.ono WHERE v.order_time >= ?1 AND v.order_time < ?2 ORDER BY v.order_time DESC, v.ono DESC, v.mno")
        .setParameter(1, filter.from()).setParameter(2, filter.until())
        .setFirstResult(filter.page * filter.size).setMaxResults(filter.size).getResultList();
    List<Map<String, Object>> result = new java.util.ArrayList<>();
    for (Object[] row : rows) {
      Map<String, Object> map = new LinkedHashMap<>();
      map.put("ono", row[0]);
      map.put("orderTime", row[1]);
      map.put("cno", row[2]);
      map.put("cname", row[3]);
      map.put("eno", row[4]);
      map.put("ename", row[5]);
      map.put("tno", row[6]);
      map.put("mno", row[7]);
      map.put("mname", row[8]);
      map.put("mtype", row[9]);
      map.put("dishCount", row[10]);
      map.put("dishPrice", row[11]);
      map.put("amount", row[12]);
      map.put("remark", row[13]);
      map.put("orderNumber", row[14] == null ? row[0] : String.format(java.util.Locale.ROOT, "%03d", ((Number) row[14]).intValue()));
      result.add(map);
    }
    long count = ((Number) em.createNativeQuery("SELECT COUNT(*) FROM v_order_detail_info WHERE order_time >= ?1 AND order_time < ?2")
        .setParameter(1, filter.from()).setParameter(2, filter.until()).getSingleResult()).longValue();
    return new PageResult<>(result, count, filter);
  }

  @GetMapping("/dish-sales-rank")
  public List<Map<String, Object>> dishSalesRank(@RequestParam(required = false) String month) {
    MonthFilter filter = new MonthFilter(month, clock);
    List<Object[]> rows = em.createNativeQuery(
        "SELECT m.mno, m.mname, m.mtype, SUM(d.dish_count) AS total_count, SUM(d.amount) AS total_amount "
        + "FROM payment p JOIN orders o ON o.ono=p.ono JOIN order_detail d ON d.ono=o.ono JOIN menu m ON m.mno=d.mno "
        + "WHERE p.status='SIMULATED_SUCCESS' AND p.paid_at >= ?1 AND p.paid_at < ?2 "
        + "GROUP BY m.mno,m.mname,m.mtype ORDER BY total_count DESC,total_amount DESC,m.mno")
        .setParameter(1, filter.from()).setParameter(2, filter.until())
        .getResultList();
    List<Map<String, Object>> result = new java.util.ArrayList<>();
    for (Object[] row : rows) {
      Map<String, Object> map = new LinkedHashMap<>();
      map.put("mno", row[0]);
      map.put("mname", row[1]);
      map.put("mtype", row[2]);
      map.put("totalCount", row[3]);
      map.put("totalAmount", row[4]);
      result.add(map);
    }
    return result;
  }

  @GetMapping("/order-summary")
  public Map<String, Object> orderSummary(@RequestParam(required = false) String month) {
    MonthFilter filter = new MonthFilter(month, clock);
    Query revenueQ = em.createNativeQuery(
        "SELECT IFNULL(SUM(amount),0) FROM payment WHERE status='SIMULATED_SUCCESS' AND paid_at >= ?1 AND paid_at < ?2")
        .setParameter(1, filter.from()).setParameter(2, filter.until());
    Query countQ = em.createNativeQuery(
        "SELECT COUNT(*) FROM orders WHERE order_time >= ?1 AND order_time < ?2")
        .setParameter(1, filter.from()).setParameter(2, filter.until());
    Object revenueValue = revenueQ.getSingleResult();
    BigDecimal revenue = revenueValue instanceof BigDecimal ? (BigDecimal) revenueValue : new BigDecimal(revenueValue.toString());
    long count = ((Number) countQ.getSingleResult()).longValue();

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("month", filter.month.toString());
    result.put("totalRevenue", revenue);
    result.put("orderCount", count);
    return result;
  }

  @GetMapping("/customer-orders")
  public PageResult<OrderQueryService.OrderRow> customerOrders(@RequestParam String cno,
      @RequestParam(required = false) String startDate, @RequestParam(required = false) String endDate,
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size,
      javax.servlet.http.HttpServletRequest request) {
    return queries.orders(new DatePageFilter(startDate, endDate, page, size, clock), SessionUser.require(request.getSession(false)), cno);
  }
}
