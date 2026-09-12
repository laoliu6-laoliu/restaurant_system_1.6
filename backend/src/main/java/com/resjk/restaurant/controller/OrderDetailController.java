package com.resjk.restaurant.controller;
import com.resjk.restaurant.model.OrderDetail;
import com.resjk.restaurant.repository.*;
import com.resjk.restaurant.service.SessionUser;
import java.util.*;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/order-details")
public class OrderDetailController {
  private final OrderDetailRepository repository;
  private final OrdersRepository orders;
  public OrderDetailController(OrderDetailRepository repository, OrdersRepository orders) {
    this.repository = repository; this.orders = orders;
  }
  @GetMapping
  public List<OrderDetail> list(@RequestParam String ono, HttpServletRequest request) {
    SessionUser user = SessionUser.require(request.getSession(false));
    com.resjk.restaurant.model.Orders order = orders.findById(ono).orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "订单不存在"));
    user.checkCustomer(order.getCno());
    return repository.findByOno(ono);
  }
  // Details are written only by transactional checkout and cannot alter a paid bill.
}
