package com.resjk.restaurant.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "administrator")
public class Administrator {
  @Id
  private String ano;
  private String aname;
  @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY)
  private String apassword;

  public String getAno() { return ano; }
  public void setAno(String ano) { this.ano = ano; }
  public String getAname() { return aname; }
  public void setAname(String aname) { this.aname = aname; }
  public String getApassword() { return apassword; }
  public void setApassword(String apassword) { this.apassword = apassword; }
}
