package com.resjk.restaurant.controller;

import com.resjk.restaurant.model.Employee;
import com.resjk.restaurant.repository.EmployeeRepository;
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
@RequestMapping("/api/employees")
public class EmployeeController {
  private final EmployeeRepository repository;

  public EmployeeController(EmployeeRepository repository) {
    this.repository = repository;
  }

  @GetMapping
  public List<Employee> list() {
    return repository.findAll();
  }

  @PostMapping
  public Employee create(@RequestBody Employee employee) {
    if (employee.getEno() == null || employee.getEno().trim().isEmpty()) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "编号不能为空");
    if (repository.existsById(employee.getEno())) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, "编号已存在");
    return repository.save(employee);
  }

  @PutMapping("/{eno}")
  @org.springframework.transaction.annotation.Transactional
  public Employee update(@PathVariable String eno, @RequestBody Employee employee) {
    Employee existing = repository.findForUpdate(eno).orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "记录不存在"));
    if (employee.getEpassword() != null && !employee.getEpassword().isEmpty()) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "请使用修改密码功能并验证旧密码");
    employee.setEpassword(existing.getEpassword());
    employee.setEno(eno);
    return repository.save(employee);
  }

  @DeleteMapping("/{eno}")
  public void delete(@PathVariable String eno) {
    repository.deleteById(eno);
  }
}
