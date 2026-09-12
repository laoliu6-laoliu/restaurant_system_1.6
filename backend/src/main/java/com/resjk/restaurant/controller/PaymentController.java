package com.resjk.restaurant.controller;
import com.resjk.restaurant.service.*;
import java.util.*;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/payments")
public class PaymentController {
  private final CheckoutService checkout;
  private final OrderQueryService queries;
  private final java.time.Clock clock;
  public PaymentController(CheckoutService checkout, OrderQueryService queries, java.time.Clock clock) { this.checkout = checkout; this.queries = queries; this.clock = clock; }
  @GetMapping("/config")
  public Map<String, Object> config() { return Map.of("mode", "DEMO", "enabled", checkout.isDemoPaymentsEnabled()); }
  @GetMapping
  public PageResult<OrderQueryService.PaymentRow> list(HttpServletRequest request,
      @RequestParam(required = false) String startDate, @RequestParam(required = false) String endDate,
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
    return queries.payments(new DatePageFilter(startDate, endDate, page, size, clock), SessionUser.require(request.getSession(false)));
  }
  public static class PaymentRequest { public String method; }
  @PostMapping("/{ono}")
  public CheckoutService.PaymentResult pay(@PathVariable String ono, @RequestBody PaymentRequest body, HttpServletRequest request) {
    return checkout.pay(ono, body.method, SessionUser.require(request.getSession(false)));
  }
}
