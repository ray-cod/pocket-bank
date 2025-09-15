package com.banking.repositories;

import com.banking.config.DbConnection;
import com.banking.models.Transaction;
import com.banking.utils.enums.TransactionType;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TransactionRepository {

    // Create transactions table if it doesn't exist
    public void createTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS transactions (
                transaction_id UUID PRIMARY KEY,
                account_id UUID NOT NULL,
                type VARCHAR(20) NOT NULL,
                amount DECIMAL(15,2) NOT NULL,
                balance_after DECIMAL(15,2) NOT NULL,
                description VARCHAR(255),
                dest_account_id UUID,
                timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (account_id) REFERENCES accounts(account_id),
                FOREIGN KEY (dest_account_id) REFERENCES accounts(account_id)
            )
        """;

        try (Connection conn = DbConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            // optional: create index for faster lookups
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_transactions_account ON transactions(account_id)");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Save a transaction
    public void save(Transaction transaction) {
        String sql = """
            INSERT INTO transactions
            (transaction_id, account_id, type, amount, balance_after, description, dest_account_id, timestamp)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = DbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, transaction.getTransactionId());
            pstmt.setObject(2, transaction.getAccountId());
            pstmt.setString(3, transaction.getType().name());
            pstmt.setBigDecimal(4, transaction.getAmount());
            pstmt.setBigDecimal(5, transaction.getBalanceAfter());
            pstmt.setString(6, transaction.getDescription());
            pstmt.setObject(7, transaction.getDestAccountId()); // handles null
            pstmt.setTimestamp(8, Timestamp.valueOf(transaction.getTimestamp()));

            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Find transactions by account ID
    public List<Transaction> findByAccountId(UUID accountId) {
        List<Transaction> transactions = new ArrayList<>();
        String sql = "SELECT * FROM transactions WHERE account_id = ? ORDER BY timestamp DESC";

        try (Connection conn = DbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, accountId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    UUID txId = rs.getObject("transaction_id", UUID.class);
                    UUID acctId = rs.getObject("account_id", UUID.class);
                    TransactionType type = TransactionType.valueOf(rs.getString("type"));
                    BigDecimal amount = rs.getBigDecimal("amount");
                    BigDecimal balanceAfter = rs.getBigDecimal("balance_after");
                    String description = rs.getString("description");
                    UUID destId = rs.getObject("dest_account_id", UUID.class);
                    Timestamp ts = rs.getTimestamp("timestamp");
                    LocalDateTime timestamp = ts != null ? ts.toLocalDateTime() : LocalDateTime.now();

                    Transaction tx = new Transaction(
                            txId, acctId,
                            type, amount,
                            balanceAfter, description,
                            destId, timestamp
                    );
                    transactions.add(tx);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return transactions;
    }

    // Get all transactions
    public List<Transaction> findAll() {
        List<Transaction> transactions = new ArrayList<>();
        String sql = "SELECT * FROM transactions ORDER BY timestamp DESC";

        try (Connection conn = DbConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                UUID txId = rs.getObject("transaction_id", UUID.class);
                UUID acctId = rs.getObject("account_id", UUID.class);
                TransactionType type = TransactionType.valueOf(rs.getString("type"));
                BigDecimal amount = rs.getBigDecimal("amount");
                BigDecimal balanceAfter = rs.getBigDecimal("balance_after");
                String description = rs.getString("description");
                UUID destId = rs.getObject("dest_account_id", UUID.class);
                Timestamp ts = rs.getTimestamp("timestamp");
                LocalDateTime timestamp = ts != null ? ts.toLocalDateTime() : LocalDateTime.now();

                Transaction tx = new Transaction(
                        txId, acctId,
                        type, amount,
                        balanceAfter, description,
                        destId, timestamp
                );
                transactions.add(tx);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return transactions;
    }
}
