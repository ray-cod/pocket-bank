package com.banking.repositories;

import com.banking.config.DbConnection;
import com.banking.models.Account;
import com.banking.models.User;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AccountRepository {

    // Create accounts table if it doesn't exist
    public void createTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS accounts (
                 account_id UUID PRIMARY KEY,
                 user_id UUID NOT NULL,
                 account_number VARCHAR(20) UNIQUE NOT NULL,
                 balance DECIMAL(15,2) DEFAULT 0,
                 currency VARCHAR(10),
                 is_active BOOLEAN DEFAULT TRUE,
                 FOREIGN KEY (user_id) REFERENCES users(user_id)
             )
        """;

        try (Connection conn = DbConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Save a new account
    public void save(Account account) {
        String sql = "INSERT INTO accounts (account_id, user_id, account_number, balance, currency, is_active) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, account.getAccountId());
            pstmt.setObject(2, account.getUserId());
            pstmt.setString(3, account.getAccountNumber());
            pstmt.setBigDecimal(4, account.getBalance());
            pstmt.setString(5, account.getCurrency());
            pstmt.setBoolean(6, account.isActive());

            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Find account by account number
    public List<Account> findByUser(User user) {
        List<Account> accounts = new ArrayList<>();
        String sql = "SELECT * FROM accounts WHERE user_id = ?";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, user.getUserId());
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                UUID accountId = rs.getObject("account_id", UUID.class);
                UUID userId = rs.getObject("user_id", UUID.class);
                String accountNumber = rs.getString("account_number");
                BigDecimal balance = rs.getBigDecimal("balance");
                String currency = rs.getString("currency");
                boolean isActive = rs.getBoolean("is_active");

                Account acc = new Account(accountId, userId, accountNumber, balance, currency, isActive);
                accounts.add(acc);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return accounts;
    }

    // Find account by account number
    public Account findByAccountNumber(String accountNumber) {
        String sql = "SELECT * FROM accounts WHERE account_number = ?";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, accountNumber);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                UUID accountId = rs.getObject("account_id", UUID.class);
                UUID userId = rs.getObject("user_id", UUID.class);
                BigDecimal balance = rs.getBigDecimal("balance");
                String currency = rs.getString("currency");
                boolean isActive = rs.getBoolean("is_active");

                return new Account(accountId, userId, accountNumber, balance, currency, isActive);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Find account by account_id
    public Account findByAccountId(UUID accountId) {
        String sql = "SELECT * FROM accounts WHERE account_id = ?";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, accountId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    UUID userId = rs.getObject("user_id", UUID.class);
                    String accountNumber = rs.getString("account_number");
                    BigDecimal balance = rs.getBigDecimal("balance");
                    String currency = rs.getString("currency");
                    boolean isActive = rs.getBoolean("is_active");

                    return new Account(accountId, userId, accountNumber, balance, currency, isActive);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Update account balance
    public void updateBalance(UUID accountId, BigDecimal newBalance) {
        String sql = "UPDATE accounts SET balance = ? WHERE account_id = ?";

        try (Connection conn = DbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setBigDecimal(1, newBalance);
            pstmt.setObject(2, accountId);

            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Get all accounts
    public List<Account> findAll() {
        List<Account> accounts = new ArrayList<>();
        String sql = "SELECT * FROM accounts";

        try (Connection conn = DbConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                UUID accountId = rs.getObject("account_id", UUID.class);
                UUID userId = rs.getObject("user_id", UUID.class);
                String accountNumber = rs.getString("account_number");
                BigDecimal balance = rs.getBigDecimal("balance");
                String currency = rs.getString("currency");
                boolean isActive = rs.getBoolean("is_active");

                Account acc = new Account(accountId, userId, accountNumber, balance, currency, isActive);
                accounts.add(acc);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return accounts;
    }

    public static void main(String[] args) {
        AccountRepository repo = new AccountRepository();
        System.out.println(repo.findAll());
    }
}
