package com.resjk.restaurant.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(90)
@ConditionalOnProperty(name = "app.upgrade-enabled", havingValue = "true", matchIfMissing = true)
public class Version09Upgrade implements ApplicationRunner {
  private final JdbcTemplate jdbc;

  public Version09Upgrade(JdbcTemplate jdbc) { this.jdbc = jdbc; }

  @Override
  public void run(ApplicationArguments args) {
    Integer columns = jdbc.queryForObject(
        "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='reservation' AND COLUMN_NAME='reservation_source'",
        Integer.class);
    if (columns == null || columns == 0) {
      jdbc.execute("ALTER TABLE reservation ADD COLUMN reservation_source VARCHAR(20)");
    }
    jdbc.update("UPDATE reservation SET reservation_source=CASE "
        + "WHEN cno IS NOT NULL AND deposit_method IN ('DEMO_WECHAT','DEMO_ALIPAY') "
        + "AND TIME(reserved_at) IN ('11:00:00','13:30:00','17:00:00','19:30:00') THEN 'ONLINE' "
        + "ELSE 'PHONE' END WHERE reservation_source IS NULL OR reservation_source='' ");
    String nullable = jdbc.queryForObject(
        "SELECT IS_NULLABLE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='reservation' AND COLUMN_NAME='reservation_source'",
        String.class);
    if ("YES".equals(nullable)) {
      jdbc.execute("ALTER TABLE reservation MODIFY COLUMN reservation_source VARCHAR(20) NOT NULL");
    }
  }
}
