package com.resjk.restaurant.model;

import java.time.LocalDate;
import javax.persistence.*;

@Entity
@Table(name = "order_sequence")
public class OrderSequence {
  @Id @Column(name = "order_date")
  private LocalDate orderDate;
  @Column(name = "last_number", nullable = false)
  private Integer lastNumber;
}
