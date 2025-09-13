package com.banking.repositories;

import com.banking.config.DbConnection;
import com.banking.models.Transaction;
import com.banking.utils.enums.TransactionType;

import java.sql.*;
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
                timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;

        try (Connection conn = DbConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(sql);

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
            pstmt.setObject(7, transaction.getDestAccountId());
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
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                UUID destId = rs.getObject("dest_account_id", UUID.class);

                Transaction transaction = (destId == null) ?
                        new Transaction(
                                rs.getObject("account_id", UUID.class),
                                TransactionType.valueOf(rs.getString("type")),
                                rs.getBigDecimal("amount"),
                                rs.getBigDecimal("balance_after"),
                                rs.getString("description")
                        ) :
                        new Transaction(
                                rs.getObject("account_id", UUID.class),
                                TransactionType.valueOf(rs.getString("type")),
                                rs.getBigDecimal("amount"),
                                rs.getBigDecimal("balance_after"),
                                rs.getString("description"),
                                destId
                        );

                transactions.add(transaction);
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
                UUID destId = rs.getObject("dest_account_id", UUID.class);

                Transaction transaction = (destId == null) ?
                        new Transaction(
                                rs.getObject("account_id", UUID.class),
                                TransactionType.valueOf(rs.getString("type")),
                                rs.getBigDecimal("amount"),
                                rs.getBigDecimal("balance_after"),
                                rs.getString("description")
                        ) :
                        new Transaction(
                                rs.getObject("account_id", UUID.class),
                                TransactionType.valueOf(rs.getString("type")),
                                rs.getBigDecimal("amount"),
                                rs.getBigDecimal("balance_after"),
                                rs.getString("description"),
                                destId
                        );

                transactions.add(transaction);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return transactions;
    }
}
