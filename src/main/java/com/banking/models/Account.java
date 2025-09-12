package com.banking.models;

import java.math.BigDecimal;
import java.util.UUID;

public class Account {

    private final UUID accountId = UUID.randomUUID();
    private final UUID userId;
    private String accountNumber = "xxx-xxx-xxx"; // ToDo: Generate Account Numbers
    private BigDecimal balance = BigDecimal.ZERO;
    private String currency = "ZAR";
    private boolean isActive = true;

    // Constructor
    public Account(User user){
        this.userId = user.getUserId();
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
