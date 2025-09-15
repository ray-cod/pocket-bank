package com.banking.repositories;

import com.banking.config.DbConnection;
import com.banking.models.User;
import com.banking.utils.enums.UserRole;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UserRepository {

    // Create users table if it doesn't exist
    public void createTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS users (
                user_id UUID PRIMARY KEY,
                username VARCHAR(50) UNIQUE NOT NULL,
                pin_hash VARCHAR(255) NOT NULL,
                locked BOOLEAN DEFAULT FALSE,
                failed_login_attempts INT DEFAULT 0,
                role VARCHAR(20) DEFAULT 'USER'
            )
        """;
        try (Connection conn = DbConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void save(User user) {
        String sql = "INSERT INTO users (user_id, username, pin_hash, locked, failed_login_attempts, role) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, user.getUserId());        // use setObject for UUID
            pstmt.setString(2, user.getUserName());
            pstmt.setString(3, user.getPinHash());
            pstmt.setBoolean(4, user.isLocked());
            pstmt.setInt(5, user.getFailedLoginAttempts());
            pstmt.setString(6, user.getRole().name());

            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public User findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    UUID userId = rs.getObject("user_id", UUID.class);
                    String uname = rs.getString("username");
                    String pinHash = rs.getString("pin_hash");
                    boolean locked = rs.getBoolean("locked");
                    int attempts = rs.getInt("failed_login_attempts");
                    UserRole role = UserRole.valueOf(rs.getString("role"));

                    // Use DB constructor so we preserve the stored UUID & hash
                    return new User(userId, uname, pinHash, locked, attempts, role);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public User findById(UUID userId) {
        String sql = "SELECT * FROM users WHERE user_id = ?";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String uname = rs.getString("username");
                    String pinHash = rs.getString("pin_hash");
                    boolean locked = rs.getBoolean("locked");
                    int attempts = rs.getInt("failed_login_attempts");
                    UserRole role = UserRole.valueOf(rs.getString("role"));

                    return new User(userId, uname, pinHash, locked, attempts, role);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void updateFailedLoginAttempts(UUID userId, int attempts) {
        String sql = "UPDATE users SET failed_login_attempts = ? WHERE user_id = ?";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, attempts);
            pstmt.setObject(2, userId);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setLocked(UUID userId, boolean locked) {
        String sql = "UPDATE users SET locked = ? WHERE user_id = ?";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setBoolean(1, locked);
            pstmt.setObject(2, userId);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Persist a newly hashed PIN (use this instead of raw JDBC in AuthService)
    public void updatePinHash(UUID userId, String newHash) {
        String sql = "UPDATE users SET pin_hash = ? WHERE user_id = ?";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newHash);
            pstmt.setObject(2, userId);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users";
        try (Connection conn = DbConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                UUID userId = rs.getObject("user_id", UUID.class);
                String uname = rs.getString("username");
                String pinHash = rs.getString("pin_hash");
                boolean locked = rs.getBoolean("locked");
                int attempts = rs.getInt("failed_login_attempts");
                UserRole role = UserRole.valueOf(rs.getString("role"));

                users.add(new User(userId, uname, pinHash, locked, attempts, role));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }
}
