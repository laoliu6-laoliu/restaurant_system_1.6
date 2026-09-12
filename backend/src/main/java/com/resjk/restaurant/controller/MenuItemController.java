package com.resjk.restaurant.controller;

import com.resjk.restaurant.model.MenuItem;
import com.resjk.restaurant.repository.MenuItemRepository;
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
@RequestMapping("/api/menu")
public class MenuItemController {
  private final MenuItemRepository repository;

  public MenuItemController(MenuItemRepository repository) {
    this.repository = repository;
  }

  @GetMapping
  public List<MenuItem> list() {
    return repository.findAll();
  }

  @PostMapping
  public MenuItem create(@RequestBody MenuItem menuItem) {
    return repository.save(menuItem);
  }

  @PutMapping("/{mno}")
  public MenuItem update(@PathVariable String mno, @RequestBody MenuItem menuItem) {
    menuItem.setMno(mno);
    return repository.save(menuItem);
  }

  @DeleteMapping("/{mno}")
  public void delete(@PathVariable String mno) {
    repository.deleteById(mno);
  }
}
