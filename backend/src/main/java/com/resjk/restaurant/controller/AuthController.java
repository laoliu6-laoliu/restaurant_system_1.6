package com.resjk.restaurant.controller;

import com.resjk.restaurant.model.Administrator;
import com.resjk.restaurant.model.Customer;
import com.resjk.restaurant.model.Employee;
import com.resjk.restaurant.repository.AdministratorRepository;
import com.resjk.restaurant.repository.CustomerRepository;
import com.resjk.restaurant.repository.EmployeeRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private final AdministratorRepository administrators;
  private final EmployeeRepository employees;
  private final CustomerRepository customers;
  private final com.resjk.restaurant.service.PasswordService passwords;

  public AuthController(
      AdministratorRepository administrators,
      EmployeeRepository employees,
      CustomerRepository customers, com.resjk.restaurant.service.PasswordService passwords) {
    this.administrators = administrators;
    this.employees = employees;
    this.customers = customers;
    this.passwords = passwords;
  }

  @PostMapping("/login")
  public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request, javax.servlet.http.HttpServletRequest httpRequest) {
    if ("admin".equals(request.getRole())) {
      Optional<Administrator> user = administrators.findById(request.getId());
      if (user.isPresent() && safeEquals(user.get().getApassword(), request.getPassword())) {
        return ok(httpRequest, "admin", user.get().getAno(), user.get().getAname());
      }
    }

    if ("staff".equals(request.getRole())) {
      Optional<Employee> user = employees.findById(request.getId());
      if (user.isPresent() && safeEquals(user.get().getEpassword(), request.getPassword())) {
        return ok(httpRequest, "staff", user.get().getEno(), user.get().getEname());
      }
    }

    if ("customer".equals(request.getRole())) {
      Optional<Customer> user = customers.findById(request.getId());
      if (user.isPresent() && safeEquals(user.get().getCpassword(), request.getPassword())) {
        return ok(httpRequest, "customer", user.get().getCno(), user.get().getCname());
      }
    }

    Map<String, Object> error = new LinkedHashMap<>();
    error.put("message", "编号或密码错误");
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
  }

  private ResponseEntity<Map<String, Object>> ok(javax.servlet.http.HttpServletRequest request, String role, String id, String name) {
    javax.servlet.http.HttpSession previous = request.getSession(false);
    if (previous != null) previous.invalidate();
    javax.servlet.http.HttpSession session = request.getSession(true);
    session.setAttribute("userId", id);
    session.setAttribute("role", role);
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("role", role);
    data.put("id", id);
    data.put("name", name);
    return ResponseEntity.ok(data);
  }

  @PostMapping("/password")
  public Map<String, String> changePassword(@RequestBody com.resjk.restaurant.service.PasswordService.ChangeRequest body, javax.servlet.http.HttpServletRequest request) {
    passwords.change(com.resjk.restaurant.service.SessionUser.require(request.getSession(false)), body);
    return Map.of("message", "密码修改成功，下次登录请使用新密码");
  }

  @PostMapping("/logout")
  public void logout(javax.servlet.http.HttpServletRequest request) {
    javax.servlet.http.HttpSession session = request.getSession(false);
    if (session != null) session.invalidate();
  }

  private boolean safeEquals(String a, String b) {
    return a != null && a.equals(b);
  }

  public static class LoginRequest {
    private String role;
    private String id;
    private String password;

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
  }
}
