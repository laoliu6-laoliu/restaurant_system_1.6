package com.resjk.restaurant.model;

import java.math.BigDecimal;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "order_detail")
public class OrderDetail {
  @Id
  private String dno;
  private String ono;
  private String mno;
  @Column(name = "dish_count")
  private Short dishCount;
  private BigDecimal amount;
  private String remark;

  public String getDno() { return dno; }
  public void setDno(String dno) { this.dno = dno; }
  public String getOno() { return ono; }
  public void setOno(String ono) { this.ono = ono; }
  public String getMno() { return mno; }
  public void setMno(String mno) { this.mno = mno; }
  public Short getDishCount() { return dishCount; }
  public void setDishCount(Short dishCount) { this.dishCount = dishCount; }
  public BigDecimal getAmount() { return amount; }
  public void setAmount(BigDecimal amount) { this.amount = amount; }
  public String getRemark() { return remark; }
  public void setRemark(String remark) { this.remark = remark; }
}
