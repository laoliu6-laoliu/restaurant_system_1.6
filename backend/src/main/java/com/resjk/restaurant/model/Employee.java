package com.resjk.restaurant.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "employee")
public class Employee {
  @Id
  private String eno;
  private String ename;
  private Short eage;
  private String esex;
  @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY)
  private String epassword;

  public String getEno() { return eno; }
  public void setEno(String eno) { this.eno = eno; }
  public String getEname() { return ename; }
  public void setEname(String ename) { this.ename = ename; }
  public Short getEage() { return eage; }
  public void setEage(Short eage) { this.eage = eage; }
  public String getEsex() { return esex; }
  public void setEsex(String esex) { this.esex = esex; }
  public String getEpassword() { return epassword; }
  public void setEpassword(String epassword) { this.epassword = epassword; }
}
