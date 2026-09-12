package com.resjk.restaurant.config;

import com.resjk.restaurant.service.DailyOrderSequence;
import java.sql.Timestamp;
import java.time.LocalDate;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@Order(30)
@ConditionalOnProperty(name = "app.upgrade-enabled", havingValue = "true", matchIfMissing = true)
public class Version03Upgrade implements ApplicationRunner {
  private final JdbcTemplate jdbc;
  private final DailyOrderSequence sequence;
  private final TransactionTemplate transactions;

  public Version03Upgrade(JdbcTemplate jdbc, DailyOrderSequence sequence, PlatformTransactionManager manager) {
    this.jdbc = jdbc; this.sequence = sequence; transactions = new TransactionTemplate(manager);
  }

  @Override
  public void run(ApplicationArguments args) {
    addColumn("order_date", "DATE");
    addColumn("daily_number", "INT");
    jdbc.execute("CREATE TABLE IF NOT EXISTS order_sequence (order_date DATE PRIMARY KEY, last_number INT NOT NULL)");
    Integer indexes = jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='orders' AND INDEX_NAME='uk_orders_date_number'", Integer.class);
    if (indexes == 0) jdbc.execute("CREATE UNIQUE INDEX uk_orders_date_number ON orders(order_date,daily_number)");
    transactions.executeWithoutResult(status -> {
      // Keep IDs and foreign keys intact; backfill only the visible daily number.
      jdbc.update("INSERT INTO order_sequence (order_date,last_number) SELECT order_date,MAX(daily_number) FROM orders WHERE order_date IS NOT NULL AND daily_number IS NOT NULL GROUP BY order_date ON DUPLICATE KEY UPDATE last_number=GREATEST(order_sequence.last_number,VALUES(last_number))");
      for (java.util.Map<String, Object> row : jdbc.queryForList("SELECT ono,order_time FROM orders WHERE daily_number IS NULL AND order_time IS NOT NULL ORDER BY order_time,ono FOR UPDATE")) {
        Object time = row.get("order_time");
        LocalDate date = time instanceof Timestamp ? ((Timestamp) time).toLocalDateTime().toLocalDate() : ((java.time.LocalDateTime) time).toLocalDate();
        jdbc.update("UPDATE orders SET order_date=?,daily_number=? WHERE ono=?", date, sequence.next(date), row.get("ono"));
      }
    });
    requireColumn("order_date", "DATE");
    requireColumn("daily_number", "INT");
  }

  private void addColumn(String column, String type) {
    Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='orders' AND COLUMN_NAME=?", Integer.class, column);
    if (count == 0) jdbc.execute("ALTER TABLE orders ADD COLUMN " + column + " " + type);
  }

  private void requireColumn(String column, String type) {
    Integer missing = jdbc.queryForObject("SELECT COUNT(*) FROM orders WHERE " + column + " IS NULL", Integer.class);
    if (missing > 0) throw new IllegalStateException("Cannot finish v0.3 upgrade: orders." + column + " contains NULL values");
    String nullable = jdbc.queryForObject("SELECT IS_NULLABLE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='orders' AND COLUMN_NAME=?", String.class, column);
    if ("YES".equals(nullable)) jdbc.execute("ALTER TABLE orders MODIFY COLUMN " + column + " " + type + " NOT NULL");
  }
}
