package com.resjk.restaurant.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "customer")
public class Customer {
  @Id
  private String cno;
  private String cname;
  private String csex;
  private String cphone;
  @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY)
  private String cpassword;

  public String getCno() { return cno; }
  public void setCno(String cno) { this.cno = cno; }
  public String getCname() { return cname; }
  public void setCname(String cname) { this.cname = cname; }
  public String getCsex() { return csex; }
  public void setCsex(String csex) { this.csex = csex; }
  public String getCphone() { return cphone; }
  public void setCphone(String cphone) { this.cphone = cphone; }
  public String getCpassword() { return cpassword; }
  public void setCpassword(String cpassword) { this.cpassword = cpassword; }
}
