package com.resjk.restaurant.controller;

import com.resjk.restaurant.model.DiningTable;
import com.resjk.restaurant.repository.DiningTableRepository;
import com.resjk.restaurant.service.ReservationService;
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
@RequestMapping("/api/tables")
public class DiningTableController {
  private final DiningTableRepository repository;
  private final ReservationService reservations;

  public DiningTableController(DiningTableRepository repository, ReservationService reservations) {
    this.repository = repository; this.reservations = reservations;
  }

  @GetMapping
  public List<DiningTable> list() {
    reservations.expireNoShows();
    reservations.syncTodayTableStatuses();
    return repository.findAll();
  }

  @PostMapping
  public DiningTable create(@RequestBody DiningTable table) {
    return repository.save(table);
  }

  @PutMapping("/{tno}")
  public DiningTable update(@PathVariable String tno, @RequestBody DiningTable table) {
    DiningTable current = repository.findById(tno).orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "餐桌不存在"));
    reservations.validateManualTableStatusChange(current.getTstatus(), table.getTstatus(), tno);
    table.setTno(tno);
    return repository.save(table);
  }

  @DeleteMapping("/{tno}")
  public void delete(@PathVariable String tno) {
    repository.deleteById(tno);
  }
}
