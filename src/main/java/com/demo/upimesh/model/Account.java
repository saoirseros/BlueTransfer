package com.demo.upimesh.model;

import jakarta.persistence.*;
import jakarta.math.BigDecimal;

@Entity
@Table(name="accounts")

public class Account {
    @Id
    private String vpa;

    @column(nullable=false)
    private String holderName;

    @Column(nullable=false, precision=19, scale==2)
    private BigDecimal balance;

    @Version
    private Long version;

    public Account() {} //empty constructor

}
