package com.resjk.restaurant.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import javax.persistence.*;

@Entity
@Table(name = "payment")
public class Payment {
  @Id
  private String paymentNo;
  @Column(nullable = false, unique = true, length = 9)
  private String ono;
  @Column(nullable = false, precision = 8, scale = 2)
  private BigDecimal amount;
  private String method;
  private String status;
  private LocalDateTime paidAt;

  public String getPaymentNo() { return paymentNo; }
  public void setPaymentNo(String value) { paymentNo = value; }
  public String getOno() { return ono; }
  public void setOno(String value) { ono = value; }
  public BigDecimal getAmount() { return amount; }
  public void setAmount(BigDecimal value) { amount = value; }
  public String getMethod() { return method; }
  public void setMethod(String value) { method = value; }
  public String getStatus() { return status; }
  public void setStatus(String value) { status = value; }
  public LocalDateTime getPaidAt() { return paidAt; }
  public void setPaidAt(LocalDateTime value) { paidAt = value; }
}
