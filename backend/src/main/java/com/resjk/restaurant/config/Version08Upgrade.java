package com.resjk.restaurant.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

@Component
@Order(80)
@ConditionalOnProperty(name = "app.upgrade-enabled", havingValue = "true", matchIfMissing = true)
public class Version08Upgrade implements ApplicationRunner {
  private final JdbcTemplate jdbc;

  public Version08Upgrade(JdbcTemplate jdbc) { this.jdbc = jdbc; }

  @Override
  public void run(ApplicationArguments args) {
    jdbc.update("INSERT INTO dining_table(tno,seats,tstatus) VALUES "
        + "('T0009',2,'空闲'),('T0010',4,'空闲'),('T0011',6,'空闲'),('T0012',8,'空闲'),"
        + "('T0013',12,'空闲'),('T0014',12,'空闲'),('T0015',12,'空闲') "
        + "ON DUPLICATE KEY UPDATE tno=dining_table.tno");
    ResourceDatabasePopulator populator = new ResourceDatabasePopulator(new ClassPathResource("menu-v0.8.sql"));
    populator.setSqlScriptEncoding("UTF-8");
    populator.execute(jdbc.getDataSource());
  }
}
