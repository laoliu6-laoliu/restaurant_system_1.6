package com.resjk.restaurant.controller;

import com.resjk.restaurant.repository.CustomerRepository;
import com.resjk.restaurant.repository.DiningTableRepository;
import com.resjk.restaurant.repository.EmployeeRepository;
import com.resjk.restaurant.repository.MenuItemRepository;
import com.resjk.restaurant.repository.OrderDetailRepository;
import com.resjk.restaurant.repository.OrdersRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
  private final EmployeeRepository employees;
  private final CustomerRepository customers;
  private final DiningTableRepository tables;
  private final MenuItemRepository menu;
  private final OrdersRepository orders;
  private final OrderDetailRepository details;

  public DashboardController(
      EmployeeRepository employees,
      CustomerRepository customers,
      DiningTableRepository tables,
      MenuItemRepository menu,
      OrdersRepository orders,
      OrderDetailRepository details) {
    this.employees = employees;
    this.customers = customers;
    this.tables = tables;
    this.menu = menu;
    this.orders = orders;
    this.details = details;
  }

  @GetMapping
  public Map<String, Long> summary() {
    Map<String, Long> data = new LinkedHashMap<>();
    data.put("employees", employees.count());
    data.put("customers", customers.count());
    data.put("tables", tables.count());
    data.put("menuItems", menu.count());
    data.put("orders", orders.count());
    data.put("orderDetails", details.count());
    return data;
  }
}
