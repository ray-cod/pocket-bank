package com.banking.models;

import com.banking.utils.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class Transaction {

    private final UUID transactionId = UUID.randomUUID();
    private final UUID accountId;
    private TransactionType type;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String description;
    private final UUID destAccountId;
    private final LocalDateTime timestamp = LocalDateTime.now();

    // Constructors
    public Transaction(UUID accountId, TransactionType type,
                       BigDecimal amount, BigDecimal balanceAfter,
                       String description) {
        this.accountId = accountId;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.description = description;
        this.destAccountId = null;
    }

    public Transaction(UUID accountId, TransactionType type,
                       BigDecimal amount, BigDecimal balanceAfter,
                       String description, UUID destAccountId) {
        this.accountId = accountId;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.description = description;
        this.destAccountId = destAccountId;
    }

    // Getters & Setters
    public UUID getTransactionId() {
        return transactionId;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public UUID getDestAccountId() {
        return destAccountId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
