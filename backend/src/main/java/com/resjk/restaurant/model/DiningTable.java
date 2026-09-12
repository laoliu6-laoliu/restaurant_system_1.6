package com.resjk.restaurant.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "dining_table")
public class DiningTable {
  @Id
  private String tno;
  private Short seats;
  private String tstatus;

  public String getTno() { return tno; }
  public void setTno(String tno) { this.tno = tno; }
  public Short getSeats() { return seats; }
  public void setSeats(Short seats) { this.seats = seats; }
  public String getTstatus() { return tstatus; }
  public void setTstatus(String tstatus) { this.tstatus = tstatus; }
}
