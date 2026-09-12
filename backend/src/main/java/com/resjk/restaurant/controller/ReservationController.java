package com.resjk.restaurant.controller;

import com.resjk.restaurant.model.Reservation;
import com.resjk.restaurant.service.*;
import java.time.LocalDate;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {
  private final ReservationService reservations;
  public ReservationController(ReservationService reservations) { this.reservations = reservations; }

  @GetMapping
  public List<Reservation> list(HttpServletRequest request) {
    reservations.expireNoShows();
    return reservations.list(SessionUser.require(request.getSession(false)));
  }

  @GetMapping("/availability")
  public List<ReservationService.AvailabilityBlock> availability(@RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate date, HttpServletRequest request) {
    reservations.expireNoShows();
    return reservations.availability(date, SessionUser.require(request.getSession(false)));
  }

  @PostMapping
  public Reservation create(@RequestBody ReservationService.CreateRequest body, HttpServletRequest request) {
    reservations.expireNoShows();
    return reservations.create(body, SessionUser.require(request.getSession(false)));
  }

  @PostMapping("/{reservationNo}/cancel")
  public Reservation cancel(@PathVariable String reservationNo, HttpServletRequest request) {
    return reservations.cancel(reservationNo, SessionUser.require(request.getSession(false)));
  }

  @PostMapping("/{reservationNo}/no-show")
  public Reservation markNoShow(@PathVariable String reservationNo, HttpServletRequest request) {
    return reservations.markNoShow(reservationNo, SessionUser.require(request.getSession(false)));
  }
}
