package com.resjk.restaurant.repository;

import com.resjk.restaurant.model.Orders;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrdersRepository extends JpaRepository<Orders, String> {
}
