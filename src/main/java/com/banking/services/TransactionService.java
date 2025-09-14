package com.banking.services;

import com.banking.models.Account;
import com.banking.models.Transaction;
import com.banking.repositories.AccountRepository;
import com.banking.repositories.TransactionRepository;
import com.banking.utils.enums.TransactionType;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class TransactionService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final DateTimeFormatter csvTimestampFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public TransactionService(AccountRepository accountRepository,
                              TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    /**
     * Return full transaction history for an account number (most recent first).
     */
    public List<Transaction> getTransactionHistory(String accountNumber) throws TransactionServiceException {
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new TransactionServiceException("Account number must be provided");
        }

        Account acc = accountRepository.findByAccountNumber(accountNumber);
        if (acc == null) throw new TransactionServiceException("Account not found: " + accountNumber);

        return transactionRepository.findByAccountId(acc.getAccountId());
    }

    /**
     * Return full transaction history for an account id.
     */
    public List<Transaction> getTransactionHistory(UUID accountId) throws TransactionServiceException {
        if (accountId == null) throw new TransactionServiceException("Account id must be provided");
        return transactionRepository.findByAccountId(accountId);
    }

    /**
     * Filter transactions by type for a given account number.
     */
    public List<Transaction> getTransactionsByType(String accountNumber, TransactionType type) throws TransactionServiceException {
        List<Transaction> all = getTransactionHistory(accountNumber);
        if (type == null) return all;
        return all.stream()
                .filter(t -> t.getType() == type)
                .collect(Collectors.toList());
    }

    /**
     * Filter transactions by a date range (inclusive) for a given account number.
     * If from is null, it's unbounded on the left; if to is null, unbounded on the right.
     */
    public List<Transaction> getTransactionsBetween(String accountNumber, LocalDateTime from, LocalDateTime to) throws TransactionServiceException {
        List<Transaction> all = getTransactionHistory(accountNumber);

        return all.stream()
                .filter(t -> {
                    LocalDateTime ts = t.getTimestamp();
                    boolean afterFrom = (from == null) || !ts.isBefore(from); // ts >= from
                    boolean beforeTo = (to == null) || !ts.isAfter(to);       // ts <= to
                    return afterFrom && beforeTo;
                })
                .collect(Collectors.toList());
    }

    /**
     * In-memory pagination: pageIndex is zero-based.
     */
    public List<Transaction> getPagedTransactions(String accountNumber, int pageIndex, int pageSize) throws TransactionServiceException {
        if (pageIndex < 0 || pageSize <= 0) throw new TransactionServiceException("Invalid page parameters");
        List<Transaction> all = getTransactionHistory(accountNumber);
        int from = pageIndex * pageSize;
        if (from >= all.size()) return Collections.emptyList();
        int to = Math.min(from + pageSize, all.size());
        return all.subList(from, to);
    }

    /**
     * Export transactions for an account to CSV. Returns the path you passed in.
     * CSV columns: transaction_id, account_id, type, amount, balance_after, description, dest_account_id, timestamp
     */
    public Path exportToCsv(String accountNumber, Path outputPath) throws TransactionServiceException {
        Objects.requireNonNull(outputPath, "outputPath cannot be null");
        List<Transaction> transactions = getTransactionHistory(accountNumber);

        try (BufferedWriter writer = Files.newBufferedWriter(outputPath)) {
            // header
            writer.write("transaction_id,account_id,type,amount,balance_after,description,dest_account_id,timestamp");
            writer.newLine();

            for (Transaction t : transactions) {
                StringBuilder sb = new StringBuilder();
                sb.append(t.getTransactionId()).append(",");
                sb.append(t.getAccountId()).append(",");
                sb.append(t.getType().name()).append(",");
                sb.append(t.getAmount()).append(",");
                sb.append(t.getBalanceAfter()).append(",");
                // escape commas/newlines in description by wrapping in quotes and doubling quotes
                String desc = t.getDescription() == null ? "" : escapeCsv(t.getDescription());
                sb.append("\"").append(desc).append("\",");
                sb.append(t.getDestAccountId() == null ? "" : t.getDestAccountId()).append(",");
                sb.append(t.getTimestamp().format(csvTimestampFormatter));
                writer.write(sb.toString());
                writer.newLine();
            }
            writer.flush();
            return outputPath;
        } catch (IOException e) {
            throw new TransactionServiceException("Failed to write CSV: " + e.getMessage(), e);
        }
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        // Replace double quotes with two double quotes
        return value.replace("\"", "\"\"");
    }

    // Custom checked exception for the service
    public static class TransactionServiceException extends Exception {
        public TransactionServiceException(String message) { super(message); }
        public TransactionServiceException(String message, Throwable cause) { super(message, cause); }
    }
}
