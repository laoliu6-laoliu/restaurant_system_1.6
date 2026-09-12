package com.resjk.restaurant.service;

import java.time.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class DatePageFilter {
  public final LocalDate startDate;
  public final LocalDate endDate;
  public final int page;
  public final int size;

  public DatePageFilter(String start, String end, int page, int size, Clock clock) {
    try {
      startDate = start == null ? (end == null ? LocalDate.now(clock) : LocalDate.parse(end)) : LocalDate.parse(start);
      endDate = end == null ? startDate : LocalDate.parse(end);
    } catch (java.time.format.DateTimeParseException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "日期格式应为 YYYY-MM-DD");
    }
    if (endDate.isBefore(startDate) || endDate.getYear() > 9998 || startDate.getYear() < 1000) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请输入有效日期，结束日期不能早于开始日期");
    if (page < 0 || page > 1000000 || size < 1 || size > 50) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "页码不能小于 0，每页应为 1 至 50 条");
    this.page = page; this.size = size;
  }
  public LocalDateTime from() { return startDate.atStartOfDay(); }
  public LocalDateTime until() { return endDate.plusDays(1).atStartOfDay(); }
}
