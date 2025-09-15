package com.banking.models;

import com.banking.utils.GeneralUtils;

import java.math.BigDecimal;
import java.util.UUID;

public class Account {

    private final UUID accountId;
    private UUID userId;
    private String accountNumber = GeneralUtils.generateAccountNumber();
    private BigDecimal balance = BigDecimal.ZERO;
    private String currency = "ZAR";
    private boolean isActive = true;

    // Constructor
    public Account(User user){
        this.accountId = UUID.randomUUID();
        this.userId = user != null? user.getUserId() : null;
    }

    // used when loading from DB
    public Account(UUID accountId, UUID userId, String accountNumber,
                   BigDecimal balance, String currency, boolean isActive) {
        // set final fields in constructor
        this.accountId = accountId;    // replace previous initialization
        this.userId = userId;
        this.accountNumber = accountNumber;
        this.balance = balance == null ? BigDecimal.ZERO : balance;
        this.currency = currency == null ? "ZAR" : currency;
        this.isActive = isActive;
    }

    // Getters & Setters
    public UUID getAccountId() {
        return accountId;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    // toString

    @Override
    public String toString() {
        return "Account{" +
                "accountId=" + accountId +
                ", userId=" + userId +
                ", accountNumber='" + accountNumber + '\'' +
                ", balance=" + balance +
                ", currency='" + currency + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}
