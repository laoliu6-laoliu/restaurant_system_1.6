package com.resjk.restaurant.controller;
import com.resjk.restaurant.model.Orders;
import com.resjk.restaurant.repository.OrdersRepository;
import com.resjk.restaurant.service.*;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/orders")
public class OrdersController {
  private final OrdersRepository repository;
  private final CheckoutService checkout;
  private final OrderQueryService queries;
  private final java.time.Clock clock;
  public OrdersController(OrdersRepository repository, CheckoutService checkout, OrderQueryService queries, java.time.Clock clock) {
    this.repository = repository; this.checkout = checkout;
    this.queries = queries; this.clock = clock;
  }
  @GetMapping
  public PageResult<OrderQueryService.OrderRow> list(HttpServletRequest request,
      @RequestParam(required = false) String startDate, @RequestParam(required = false) String endDate,
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
    SessionUser user = SessionUser.require(request.getSession(false));
    return queries.orders(new DatePageFilter(startDate, endDate, page, size, clock), user, null);
  }
  @PostMapping({"", "/checkout"})
  public Orders create(@RequestBody CheckoutService.CheckoutRequest order, HttpServletRequest request) {
    return checkout.checkout(order, SessionUser.require(request.getSession(false)));
  }
  @PostMapping("/transfer")
  public CheckoutService.TableTransferResult transfer(@RequestBody CheckoutService.TableTransferRequest transfer, HttpServletRequest request) {
    return checkout.transferTable(transfer, SessionUser.require(request.getSession(false)));
  }
  @PostMapping("/{ono}/end")
  public Orders end(@PathVariable String ono, HttpServletRequest request) {
    return checkout.endOrder(ono, SessionUser.require(request.getSession(false)));
  }
  @DeleteMapping("/{ono}")
  public void delete(@PathVariable String ono, HttpServletRequest request) {
    checkout.deleteEnded(ono, SessionUser.require(request.getSession(false)));
  }
}
