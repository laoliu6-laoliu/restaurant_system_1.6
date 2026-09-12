package com.resjk.restaurant.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(150)
@ConditionalOnProperty(name = "app.upgrade-enabled", havingValue = "true", matchIfMissing = true)
public class Version15Upgrade implements ApplicationRunner {
  private final JdbcTemplate jdbc;

  public Version15Upgrade(JdbcTemplate jdbc) { this.jdbc = jdbc; }

  @Override
  public void run(ApplicationArguments args) {
    addColumn("order_status", "VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'");
    addColumn("ended_at", "DATETIME NULL");
    jdbc.update("UPDATE orders SET order_status='ACTIVE' WHERE order_status IS NULL OR order_status='' ");
  }

  private void addColumn(String column, String definition) {
    Integer count = jdbc.queryForObject(
        "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='orders' AND COLUMN_NAME=?",
        Integer.class, column);
    if (count == null || count == 0) jdbc.execute("ALTER TABLE orders ADD COLUMN " + column + " " + definition);
  }
}
