package com.resjk.restaurant.repository;

import com.resjk.restaurant.model.OrderDetail;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderDetailRepository extends JpaRepository<OrderDetail, String> {
  List<OrderDetail> findByOno(String ono);
}
