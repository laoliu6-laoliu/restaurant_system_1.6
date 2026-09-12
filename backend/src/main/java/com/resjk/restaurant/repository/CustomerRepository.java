package com.resjk.restaurant.repository;

import com.resjk.restaurant.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, String> {
  @org.springframework.data.jpa.repository.Lock(javax.persistence.LockModeType.PESSIMISTIC_WRITE)
  @org.springframework.data.jpa.repository.Query("select a from Customer a where a.cno = :id")
  java.util.Optional<Customer> findForUpdate(@org.springframework.data.repository.query.Param("id") String id);
}
