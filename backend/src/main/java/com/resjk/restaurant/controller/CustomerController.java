package com.resjk.restaurant.controller;

import com.resjk.restaurant.model.Customer;
import com.resjk.restaurant.repository.CustomerRepository;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {
  private final CustomerRepository repository;

  public CustomerController(CustomerRepository repository) {
    this.repository = repository;
  }

  @GetMapping
  public List<Customer> list(javax.servlet.http.HttpServletRequest request) {
    com.resjk.restaurant.service.SessionUser user = com.resjk.restaurant.service.SessionUser.require(request.getSession(false));
    if ("customer".equals(user.role)) return repository.findById(user.id).map(java.util.Collections::singletonList).orElse(java.util.Collections.emptyList());
    return repository.findAll();
  }

  @PostMapping
  public Customer create(@RequestBody Customer customer) {
    if (customer.getCno() == null || customer.getCno().trim().isEmpty()) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "编号不能为空");
    if (repository.existsById(customer.getCno())) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, "编号已存在");
    return repository.save(customer);
  }

  @PutMapping("/{cno}")
  @org.springframework.transaction.annotation.Transactional
  public Customer update(@PathVariable String cno, @RequestBody Customer customer) {
    Customer existing = repository.findForUpdate(cno).orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "记录不存在"));
    if (customer.getCpassword() != null && !customer.getCpassword().isEmpty()) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "请使用修改密码功能并验证旧密码");
    customer.setCpassword(existing.getCpassword());
    customer.setCno(cno);
    return repository.save(customer);
  }

  @DeleteMapping("/{cno}")
  public void delete(@PathVariable String cno) {
    repository.deleteById(cno);
  }
}
