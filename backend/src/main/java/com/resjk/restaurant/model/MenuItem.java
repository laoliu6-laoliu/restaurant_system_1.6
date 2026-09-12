package com.resjk.restaurant.model;

import java.math.BigDecimal;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "menu")
public class MenuItem {
  @Id
  private String mno;
  private String mname;
  private String mtype;
  private BigDecimal mprice;
  private Integer stock;
  @javax.persistence.Column(name = "image_url", length = 500)
  private String imageUrl;

  public String getImageUrl() { return imageUrl; }
  public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

  public String getMno() { return mno; }
  public void setMno(String mno) { this.mno = mno; }
  public String getMname() { return mname; }
  public void setMname(String mname) { this.mname = mname; }
  public String getMtype() { return mtype; }
  public void setMtype(String mtype) { this.mtype = mtype; }
  public BigDecimal getMprice() { return mprice; }
  public void setMprice(BigDecimal mprice) { this.mprice = mprice; }
  public Integer getStock() { return stock; }
  public void setStock(Integer stock) { this.stock = stock; }
}
