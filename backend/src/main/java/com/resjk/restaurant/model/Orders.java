package com.resjk.restaurant.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "orders", uniqueConstraints = @javax.persistence.UniqueConstraint(name = "uk_orders_date_number", columnNames = {"order_date", "daily_number"}))
public class Orders {
  @Id
  private String ono;
  @Column(name = "total_amount")
  private BigDecimal totalAmount;
  @Column(name = "order_time")
  private LocalDateTime orderTime;
  private String eno;
  private String cno;
  private String tno;
  @Column(name = "order_date")
  private java.time.LocalDate orderDate;
  @Column(name = "daily_number")
  private Integer dailyNumber;

  public java.time.LocalDate getOrderDate() { return orderDate; }
  public void setOrderDate(java.time.LocalDate value) { orderDate = value; }
  public Integer getDailyNumber() { return dailyNumber; }
  public void setDailyNumber(Integer value) { dailyNumber = value; }
  public String getOrderNumber() { return dailyNumber == null ? ono : String.format(java.util.Locale.ROOT, "%03d", dailyNumber); }
  @Column(name = "payment_status", nullable = false)
  @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY)
  private String paymentStatus = "UNPAID";
  @Column(name = "order_status", nullable = false)
  @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY)
  private String orderStatus = "ACTIVE";
  @Column(name = "ended_at")
  @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY)
  private LocalDateTime endedAt;

  public String getPaymentStatus() { return paymentStatus; }
  public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
  public String getOrderStatus() { return orderStatus; }
  public void setOrderStatus(String orderStatus) { this.orderStatus = orderStatus; }
  public LocalDateTime getEndedAt() { return endedAt; }
  public void setEndedAt(LocalDateTime endedAt) { this.endedAt = endedAt; }

  public String getOno() { return ono; }
  public void setOno(String ono) { this.ono = ono; }
  public BigDecimal getTotalAmount() { return totalAmount; }
  public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
  public LocalDateTime getOrderTime() { return orderTime; }
  public void setOrderTime(LocalDateTime orderTime) { this.orderTime = orderTime; }
  public String getEno() { return eno; }
  public void setEno(String eno) { this.eno = eno; }
  public String getCno() { return cno; }
  public void setCno(String cno) { this.cno = cno; }
  public String getTno() { return tno; }
  public void setTno(String tno) { this.tno = tno; }
}
