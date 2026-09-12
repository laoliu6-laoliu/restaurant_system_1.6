package com.resjk.restaurant.service;

import java.time.LocalDate;
import java.util.List;

public class PageResult<T> {
  public final List<T> content;
  public final long totalElements;
  public final long totalPages;
  public final int page;
  public final int size;
  public final LocalDate startDate;
  public final LocalDate endDate;

  public PageResult(List<T> content, long count, DatePageFilter filter) {
    this.content = content; totalElements = count; totalPages = (count + filter.size - 1) / filter.size;
    page = filter.page; size = filter.size; startDate = filter.startDate; endDate = filter.endDate;
  }
}
