package com.resjk.restaurant.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class MonthFilter {
  public final YearMonth month;

  public MonthFilter(String value, Clock clock) {
    try {
      month = value == null || value.trim().isEmpty() ? YearMonth.now(clock) : YearMonth.parse(value);
    } catch (DateTimeParseException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "月份格式应为 YYYY-MM");
    }
    if (month.getYear() < 1000 || month.getYear() > 9998) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请输入有效月份");
    }
  }

  public LocalDateTime from() { return month.atDay(1).atStartOfDay(); }
  public LocalDateTime until() { return month.plusMonths(1).atDay(1).atStartOfDay(); }
}
