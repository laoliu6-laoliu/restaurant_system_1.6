package com.resjk.restaurant.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import javax.persistence.*;

@Entity
@Table(name = "reservation")
public class Reservation {
  @Id
  @Column(name = "reservation_no", length = 40)
  private String reservationNo;
  @Column(name = "customer_name", nullable = false, length = 30)
  private String customerName;
  @Column(name = "customer_phone", nullable = false, length = 20)
  private String customerPhone;
  @Column(length = 9)
  private String cno;
  @Column(nullable = false, length = 9)
  private String tno;
  @Column(nullable = false, length = 9)
  private String eno;
  @Column(name = "party_size", nullable = false)
  private Short partySize;
  @Column(name = "reservation_source", nullable = false, length = 20)
  private String reservationSource;
  @Column(name = "reserved_at", nullable = false)
  private LocalDateTime reservedAt;
  @Column(name = "reserved_until", nullable = false)
  private LocalDateTime reservedUntil;
  @Column(name = "arrival_deadline", nullable = false)
  private LocalDateTime arrivalDeadline;
  @Column(name = "deposit_amount", nullable = false, precision = 8, scale = 2)
  private BigDecimal depositAmount;
  @Column(name = "deposit_method", nullable = false, length = 30)
  private String depositMethod;
  @Column(name = "deposit_status", nullable = false, length = 30)
  private String depositStatus;
  @Column(name = "deposit_paid_at", nullable = false)
  private LocalDateTime depositPaidAt;
  @Column(name = "deposit_refunded_at")
  private LocalDateTime depositRefundedAt;
  @Column(name = "deposit_forfeited_at")
  private LocalDateTime depositForfeitedAt;
  @Column(nullable = false, length = 30)
  private String status;
  @Column(length = 9, unique = true)
  private String ono;
  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  public String getReservationNo() { return reservationNo; }
  public void setReservationNo(String value) { reservationNo = value; }
  public String getCustomerName() { return customerName; }
  public void setCustomerName(String value) { customerName = value; }
  public String getCustomerPhone() { return customerPhone; }
  public void setCustomerPhone(String value) { customerPhone = value; }
  public String getCno() { return cno; }
  public void setCno(String value) { cno = value; }
  public String getTno() { return tno; }
  public void setTno(String value) { tno = value; }
  public String getEno() { return eno; }
  public void setEno(String value) { eno = value; }
  public Short getPartySize() { return partySize; }
  public void setPartySize(Short value) { partySize = value; }
  public String getReservationSource() { return reservationSource; }
  public void setReservationSource(String value) { reservationSource = value; }
  public LocalDateTime getReservedAt() { return reservedAt; }
  public void setReservedAt(LocalDateTime value) { reservedAt = value; }
  public LocalDateTime getReservedUntil() { return reservedUntil; }
  public void setReservedUntil(LocalDateTime value) { reservedUntil = value; }
  public LocalDateTime getArrivalDeadline() { return arrivalDeadline; }
  public void setArrivalDeadline(LocalDateTime value) { arrivalDeadline = value; }
  public BigDecimal getDepositAmount() { return depositAmount; }
  public void setDepositAmount(BigDecimal value) { depositAmount = value; }
  public String getDepositMethod() { return depositMethod; }
  public void setDepositMethod(String value) { depositMethod = value; }
  public String getDepositStatus() { return depositStatus; }
  public void setDepositStatus(String value) { depositStatus = value; }
  public LocalDateTime getDepositPaidAt() { return depositPaidAt; }
  public void setDepositPaidAt(LocalDateTime value) { depositPaidAt = value; }
  public LocalDateTime getDepositRefundedAt() { return depositRefundedAt; }
  public void setDepositRefundedAt(LocalDateTime value) { depositRefundedAt = value; }
  public LocalDateTime getDepositForfeitedAt() { return depositForfeitedAt; }
  public void setDepositForfeitedAt(LocalDateTime value) { depositForfeitedAt = value; }
  public String getStatus() { return status; }
  public void setStatus(String value) { status = value; }
  public String getOno() { return ono; }
  public void setOno(String value) { ono = value; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime value) { createdAt = value; }
}
