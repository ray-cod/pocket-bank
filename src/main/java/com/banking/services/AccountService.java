package com.banking.services;

import com.banking.models.Account;
import com.banking.models.Transaction;
import com.banking.repositories.AccountRepository;
import com.banking.config.DbConnection;
import com.banking.repositories.TransactionRepository;
import com.banking.utils.enums.TransactionType;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class AccountService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    // configurable minimum and maximums could be added here
    public AccountService(AccountRepository accountRepository,
                          TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    // Create a new account and persist it
    public Account createAccount(com.banking.models.User user, BigDecimal initialBalance) {
        Account account = new Account(user);
        if (initialBalance != null) {
            account.setBalance(initialBalance);
        }
        accountRepository.save(account);
        // Optionally create an initial deposit transaction
        if (initialBalance != null && initialBalance.compareTo(BigDecimal.ZERO) > 0) {
            Transaction tx = new Transaction(
                    account.getAccountId(),
                    TransactionType.DEPOSIT,
                    initialBalance,
                    account.getBalance(),
                    "Initial deposit"
            );
            transactionRepository.save(tx);
        }
        return account;
    }

    // Retrieve account by account number
    public Account getAccountByNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber);
    }

    // Return current balance
    public BigDecimal getBalance(String accountNumber) throws AccountServiceException {
        Account acc = getAccountByNumber(accountNumber);
        if (acc == null) throw new AccountServiceException("Account not found");
        return acc.getBalance();
    }

    // Deposit money (atomic)
    public Transaction deposit(String accountNumber, BigDecimal amount, String description) throws AccountServiceException {
        validateAmount(amount);
        try (Connection conn = DbConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Update balance atomically: balance = balance + ?
                String updateSql = "UPDATE accounts SET balance = balance + ? WHERE account_number = ?";
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setBigDecimal(1, amount);
                    updateStmt.setString(2, accountNumber);
                    int updated = updateStmt.executeUpdate();
                    if (updated == 0) {
                        conn.rollback();
                        throw new AccountServiceException("Account not found: " + accountNumber);
                    }
                }

                // Fetch account_id and new balance
                String selectSql = "SELECT account_id, balance FROM accounts WHERE account_number = ?";
                UUID accountId;
                BigDecimal newBalance;
                try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                    selectStmt.setString(1, accountNumber);
                    try (ResultSet rs = selectStmt.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            throw new AccountServiceException("Account not found after update");
                        }
                        accountId = rs.getObject("account_id", UUID.class);
                        newBalance = rs.getBigDecimal("balance");
                    }
                }

                // Insert transaction row
                String insertSql = """
                    INSERT INTO transactions
                    (transaction_id, account_id, type, amount, balance_after, description, dest_account_id, timestamp)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """;
                try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                    UUID txId = UUID.randomUUID();
                    insertStmt.setObject(1, txId);
                    insertStmt.setObject(2, accountId);
                    insertStmt.setString(3, TransactionType.DEPOSIT.name());
                    insertStmt.setBigDecimal(4, amount);
                    insertStmt.setBigDecimal(5, newBalance);
                    insertStmt.setString(6, description);
                    insertStmt.setObject(7, null);
                    insertStmt.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));
                    insertStmt.executeUpdate();

                    // Build Transaction object to return
                    Transaction tx = new Transaction(accountId, TransactionType.DEPOSIT, amount, newBalance, description);
                    conn.commit();
                    return tx;
                }
            } catch (SQLException e) {
                conn.rollback();
                throw new AccountServiceException("Failed to deposit: " + e.getMessage(), e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new AccountServiceException("DB connection error: " + e.getMessage(), e);
        }
    }

    // Withdraw money (atomic, checks sufficient balance)
    public Transaction withdraw(String accountNumber, BigDecimal amount, String description) throws AccountServiceException {
        validateAmount(amount);
        try (Connection conn = DbConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Read current balance FOR UPDATE pattern (we do select then update within the same transaction)
                String selectSql = "SELECT account_id, balance FROM accounts WHERE account_number = ?";
                UUID accountId;
                BigDecimal currentBalance;
                try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                    selectStmt.setString(1, accountNumber);
                    try (ResultSet rs = selectStmt.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            throw new AccountServiceException("Account not found: " + accountNumber);
                        }
                        accountId = rs.getObject("account_id", UUID.class);
                        currentBalance = rs.getBigDecimal("balance");
                    }
                }

                if (currentBalance.compareTo(amount) < 0) {
                    conn.rollback();
                    throw new AccountServiceException("Insufficient funds");
                }

                // Update balance = balance - amount
                String updateSql = "UPDATE accounts SET balance = balance - ? WHERE account_id = ?";
                try (PreparedStatement upd = conn.prepareStatement(updateSql)) {
                    upd.setBigDecimal(1, amount);
                    upd.setObject(2, accountId);
                    upd.executeUpdate();
                }

                // Fetch new balance
                BigDecimal newBalance;
                try (PreparedStatement s2 = conn.prepareStatement("SELECT balance FROM accounts WHERE account_id = ?")) {
                    s2.setObject(1, accountId);
                    try (ResultSet rs2 = s2.executeQuery()) {
                        rs2.next();
                        newBalance = rs2.getBigDecimal("balance");
                    }
                }

                // Insert transaction
                String insertSql = """
                    INSERT INTO transactions
                    (transaction_id, account_id, type, amount, balance_after, description, dest_account_id, timestamp)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """;
                try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                    UUID txId = UUID.randomUUID();
                    insertStmt.setObject(1, txId);
                    insertStmt.setObject(2, accountId);
                    insertStmt.setString(3, TransactionType.WITHDRAW.name());
                    insertStmt.setBigDecimal(4, amount);
                    insertStmt.setBigDecimal(5, newBalance);
                    insertStmt.setString(6, description);
                    insertStmt.setObject(7, null);
                    insertStmt.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));
                    insertStmt.executeUpdate();

                    Transaction tx = new Transaction(accountId, TransactionType.WITHDRAW, amount, newBalance, description);
                    conn.commit();
                    return tx;
                }

            } catch (SQLException e) {
                conn.rollback();
                throw new AccountServiceException("Failed to withdraw: " + e.getMessage(), e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new AccountServiceException("DB connection error: " + e.getMessage(), e);
        }
    }

    // Transfer money between two accounts (atomic)
    public Transaction[] transfer(String fromAccountNumber, String toAccountNumber, BigDecimal amount, String description) throws AccountServiceException {
        if (fromAccountNumber.equals(toAccountNumber)) {
            throw new AccountServiceException("Source and destination accounts must differ");
        }
        validateAmount(amount);

        try (Connection conn = DbConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1) Fetch both accounts and balances
                String selectSql = "SELECT account_id, balance FROM accounts WHERE account_number = ?";
                UUID fromId;
                UUID toId;
                BigDecimal fromBalance;
                BigDecimal toBalance;

                try (PreparedStatement s = conn.prepareStatement(selectSql)) {
                    // from
                    s.setString(1, fromAccountNumber);
                    try (ResultSet rs = s.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            throw new AccountServiceException("Source account not found");
                        }
                        fromId = rs.getObject("account_id", UUID.class);
                        fromBalance = rs.getBigDecimal("balance");
                    }
                }

                try (PreparedStatement s = conn.prepareStatement(selectSql)) {
                    // to
                    s.setString(1, toAccountNumber);
                    try (ResultSet rs = s.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            throw new AccountServiceException("Destination account not found");
                        }
                        toId = rs.getObject("account_id", UUID.class);
                        toBalance = rs.getBigDecimal("balance");
                    }
                }

                if (fromBalance.compareTo(amount) < 0) {
                    conn.rollback();
                    throw new AccountServiceException("Insufficient funds in source account");
                }

                // 2) Update balances
                String debitSql = "UPDATE accounts SET balance = balance - ? WHERE account_id = ?";
                try (PreparedStatement debitStmt = conn.prepareStatement(debitSql)) {
                    debitStmt.setBigDecimal(1, amount);
                    debitStmt.setObject(2, fromId);
                    debitStmt.executeUpdate();
                }

                String creditSql = "UPDATE accounts SET balance = balance + ? WHERE account_id = ?";
                try (PreparedStatement creditStmt = conn.prepareStatement(creditSql)) {
                    creditStmt.setBigDecimal(1, amount);
                    creditStmt.setObject(2, toId);
                    creditStmt.executeUpdate();
                }

                // 3) Read updated balances
                BigDecimal newFromBalance;
                BigDecimal newToBalance;
                try (PreparedStatement s = conn.prepareStatement("SELECT balance FROM accounts WHERE account_id = ?")) {
                    s.setObject(1, fromId);
                    try (ResultSet rs = s.executeQuery()) {
                        rs.next();
                        newFromBalance = rs.getBigDecimal("balance");
                    }
                }
                try (PreparedStatement s = conn.prepareStatement("SELECT balance FROM accounts WHERE account_id = ?")) {
                    s.setObject(1, toId);
                    try (ResultSet rs = s.executeQuery()) {
                        rs.next();
                        newToBalance = rs.getBigDecimal("balance");
                    }
                }

                // 4) Insert two transaction records (transfer out and transfer in)
                String insertSql = """
                    INSERT INTO transactions
                    (transaction_id, account_id, type, amount, balance_after, description, dest_account_id, timestamp)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """;

                // source transaction (TRANSFER_OUT)
                try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                    UUID txId = UUID.randomUUID();
                    insertStmt.setObject(1, txId);
                    insertStmt.setObject(2, fromId);
                    insertStmt.setString(3, TransactionType.TRANSFER_OUT.name());
                    insertStmt.setBigDecimal(4, amount);
                    insertStmt.setBigDecimal(5, newFromBalance);
                    insertStmt.setString(6, description);
                    insertStmt.setObject(7, toId);
                    insertStmt.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));
                    insertStmt.executeUpdate();
                }

                // dest transaction (TRANSFER_IN)
                try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                    UUID txId = UUID.randomUUID();
                    insertStmt.setObject(1, txId);
                    insertStmt.setObject(2, toId);
                    insertStmt.setString(3, TransactionType.TRANSFER_IN.name());
                    insertStmt.setBigDecimal(4, amount);
                    insertStmt.setBigDecimal(5, newToBalance);
                    insertStmt.setString(6, description);
                    insertStmt.setObject(7, fromId);
                    insertStmt.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));
                    insertStmt.executeUpdate();
                }

                // Build Transaction objects to return
                Transaction outTx = new Transaction(fromId, TransactionType.TRANSFER_OUT, amount, newFromBalance, description, toId);
                Transaction inTx = new Transaction(toId, TransactionType.TRANSFER_IN, amount, newToBalance, description, fromId);

                conn.commit();
                return new Transaction[] { outTx, inTx };

            } catch (SQLException e) {
                conn.rollback();
                throw new AccountServiceException("Failed to perform transfer: " + e.getMessage(), e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new AccountServiceException("DB connection error: " + e.getMessage(), e);
        }
    }

    // Fetch transaction history for an account number
    public List<Transaction> getTransactionHistory(String accountNumber) throws AccountServiceException {
        Account acc = accountRepository.findByAccountNumber(accountNumber);
        if (acc == null) throw new AccountServiceException("Account not found");
        return transactionRepository.findByAccountId(acc.getAccountId());
    }

    // Helper to validate amount
    private void validateAmount(BigDecimal amount) throws AccountServiceException {
        if (amount == null) throw new AccountServiceException("Amount cannot be null");
        if (amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new AccountServiceException("Amount must be greater than zero");
    }

    // Custom exception
    public static class AccountServiceException extends Exception {
        public AccountServiceException(String message) { super(message); }
        public AccountServiceException(String message, Throwable cause) { super(message, cause); }
    }
}
