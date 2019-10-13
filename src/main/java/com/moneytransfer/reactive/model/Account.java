package com.moneytransfer.reactive.model;

import lombok.*;

import java.math.BigDecimal;
import java.util.Currency;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
/**
 * A simple POJO class to hold account information
 *
 * @author Julian Vasa
 */
public class Account {

    private int id;
    private String name;
    private BigDecimal balance;
    private Currency currency;

    /**
     * Withdraw an amount of money from the account => balance = balance - amount
     * @param amount
     */
    public void withdraw(BigDecimal amount) {
        this.balance = balance.subtract(amount);
    }

    /**
     * Deposit an amount of money in the account => balance = balance + amount
     * @param amount
     */
    public void deposit(BigDecimal amount) {
        this.balance = balance.add(amount);
    }
}
