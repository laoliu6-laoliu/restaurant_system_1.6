package com.resjk.restaurant.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

@org.springframework.core.annotation.Order(20)
@Component
@ConditionalOnProperty(name = "app.upgrade-enabled", havingValue = "true", matchIfMissing = true)
public class Version02Upgrade implements ApplicationRunner {
  private final JdbcTemplate jdbc;
  public Version02Upgrade(JdbcTemplate jdbc) { this.jdbc = jdbc; }

  @Override
  public void run(ApplicationArguments args) {
    addColumn("menu", "stock", "INT DEFAULT 50");
    addColumn("menu", "image_url", "VARCHAR(500)");
    addColumn("orders", "payment_status", "VARCHAR(20) NOT NULL DEFAULT 'UNPAID'");
    jdbc.execute("CREATE TABLE IF NOT EXISTS payment (payment_no VARCHAR(40) PRIMARY KEY, ono CHAR(9) NOT NULL UNIQUE, "
        + "amount DECIMAL(8,2) NOT NULL, method VARCHAR(30) NOT NULL, status VARCHAR(30) NOT NULL, paid_at DATETIME NOT NULL, "
        + "CONSTRAINT fk_payment_order FOREIGN KEY (ono) REFERENCES orders(ono))");
    ResourceDatabasePopulator populator = new ResourceDatabasePopulator(new ClassPathResource("menu-v0.2.sql"));
    populator.setSqlScriptEncoding("UTF-8");
    populator.execute(jdbc.getDataSource());
  }

  private void addColumn(String table, String column, String definition) {
    Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?", Integer.class, table, column);
    if (count == null || count == 0) jdbc.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
  }
}
