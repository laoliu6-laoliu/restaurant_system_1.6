package com.resjk.restaurant.config;

import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(70)
@ConditionalOnProperty(name = "app.upgrade-enabled", havingValue = "true", matchIfMissing = true)
public class Version07Upgrade implements ApplicationRunner {
  private final JdbcTemplate jdbc;
  public Version07Upgrade(JdbcTemplate jdbc) { this.jdbc = jdbc; }

  @Override
  public void run(ApplicationArguments args) {
    addColumn("reserved_until", "DATETIME");
    addColumn("arrival_deadline", "DATETIME");
    addColumn("deposit_forfeited_at", "DATETIME");
    jdbc.update("UPDATE reservation SET reserved_until=DATE_ADD(reserved_at, INTERVAL 2 HOUR) WHERE reserved_until IS NULL");
    jdbc.update("UPDATE reservation SET arrival_deadline=DATE_ADD(reserved_at, INTERVAL 30 MINUTE) WHERE arrival_deadline IS NULL");
    requireColumn("reserved_until", "DATETIME");
    requireColumn("arrival_deadline", "DATETIME");
    Integer indexes = jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='reservation' AND INDEX_NAME='idx_reservation_arrival_deadline'", Integer.class);
    if (indexes == 0) jdbc.execute("CREATE INDEX idx_reservation_arrival_deadline ON reservation(status,arrival_deadline)");
  }

  private void addColumn(String column, String type) {
    Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='reservation' AND COLUMN_NAME=?", Integer.class, column);
    if (count == 0) jdbc.execute("ALTER TABLE reservation ADD COLUMN " + column + " " + type);
  }

  private void requireColumn(String column, String type) {
    Integer missing = jdbc.queryForObject("SELECT COUNT(*) FROM reservation WHERE " + column + " IS NULL", Integer.class);
    if (missing > 0) throw new IllegalStateException("Cannot finish v0.7 upgrade: reservation." + column + " contains NULL values");
    String nullable = jdbc.queryForObject("SELECT IS_NULLABLE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='reservation' AND COLUMN_NAME=?", String.class, column);
    if ("YES".equals(nullable)) jdbc.execute("ALTER TABLE reservation MODIFY COLUMN " + column + " " + type + " NOT NULL");
  }
}
