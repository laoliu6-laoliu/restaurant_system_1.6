package com.resjk.restaurant.repository;

import com.resjk.restaurant.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<Employee, String> {
  @org.springframework.data.jpa.repository.Lock(javax.persistence.LockModeType.PESSIMISTIC_WRITE)
  @org.springframework.data.jpa.repository.Query("select a from Employee a where a.eno = :id")
  java.util.Optional<Employee> findForUpdate(@org.springframework.data.repository.query.Param("id") String id);
}
