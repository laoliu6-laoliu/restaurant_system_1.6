package com.resjk.restaurant.config;

import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(50)
@ConditionalOnProperty(name = "app.upgrade-enabled", havingValue = "true", matchIfMissing = true)
public class Version05Upgrade implements ApplicationRunner {
  private final JdbcTemplate jdbc;
  public Version05Upgrade(JdbcTemplate jdbc) { this.jdbc = jdbc; }

  @Override
  public void run(ApplicationArguments args) {
    jdbc.execute("CREATE TABLE IF NOT EXISTS reservation ("
        + "reservation_no VARCHAR(40) PRIMARY KEY,customer_name VARCHAR(30) NOT NULL,customer_phone VARCHAR(20) NOT NULL,"
        + "cno CHAR(9),tno CHAR(9) NOT NULL,eno CHAR(9) NOT NULL,party_size SMALLINT NOT NULL,reserved_at DATETIME NOT NULL,"
        + "deposit_amount DECIMAL(8,2) NOT NULL,deposit_method VARCHAR(30) NOT NULL,deposit_status VARCHAR(30) NOT NULL,"
        + "deposit_paid_at DATETIME NOT NULL,deposit_refunded_at DATETIME,status VARCHAR(30) NOT NULL,ono CHAR(9) UNIQUE,created_at DATETIME NOT NULL,"
        + "INDEX idx_reservation_table_status(tno,status),INDEX idx_reservation_phone(customer_phone),INDEX idx_reservation_time(reserved_at),"
        + "CONSTRAINT fk_reservation_customer FOREIGN KEY(cno) REFERENCES customer(cno),"
        + "CONSTRAINT fk_reservation_table FOREIGN KEY(tno) REFERENCES dining_table(tno),"
        + "CONSTRAINT fk_reservation_employee FOREIGN KEY(eno) REFERENCES employee(eno),"
        + "CONSTRAINT fk_reservation_order FOREIGN KEY(ono) REFERENCES orders(ono))");
    jdbc.update("UPDATE dining_table t LEFT JOIN reservation r ON r.tno=t.tno AND r.status IN ('RESERVED','SEATED') "
        + "SET t.tstatus='空闲' WHERE t.tstatus='已预订' AND r.reservation_no IS NULL");
  }
}
