package com.resjk.restaurant.service;

import java.time.LocalDate;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DailyOrderSequence {
  private final JdbcTemplate jdbc;
  public DailyOrderSequence(JdbcTemplate jdbc) { this.jdbc = jdbc; }

  @Transactional
  public int next(LocalDate day) {
    // The upsert also serializes the first two orders on a new date.
    jdbc.update("INSERT INTO order_sequence (order_date,last_number) VALUES (?,0) ON DUPLICATE KEY UPDATE last_number=last_number", day);
    int current = jdbc.queryForObject("SELECT last_number FROM order_sequence WHERE order_date=? FOR UPDATE", Integer.class, day);
    if (current >= 999) throw new ResponseStatusException(HttpStatus.CONFLICT, "当天订单已达到 999 单上限，请于次日再下单");
    jdbc.update("UPDATE order_sequence SET last_number=? WHERE order_date=?", current + 1, day);
    return current + 1;
  }
}
