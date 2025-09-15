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

    // Save new user
    public void save(User user) {
        String sql = "INSERT INTO users (user_id, username, pin_hash, locked, failed_login_attempts, role) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, user.getUserId());
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

    // Find user by username
    public User findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                User user = new User(rs.getString("username"), "0000"); // pin will be overwritten
                user.setLocked(rs.getBoolean("locked"));
                user.setFailedLoginAttempts(rs.getInt("failed_login_attempts"));
                user.setRole(UserRole.valueOf(rs.getString("role")));

                // Overwrite pin hash
                user.updatePinHashDirectly(rs.getString("pin_hash")); // helper method, see below
                return user;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Update failed login attempts
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

    // Lock or unlock account
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

    // Get all users
    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users";

        try (Connection conn = DbConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                User user = new User(rs.getString("username"), "dummy");
                user.setLocked(rs.getBoolean("locked"));
                user.setFailedLoginAttempts(rs.getInt("failed_login_attempts"));
                user.setRole(UserRole.valueOf(rs.getString("role")));
                user.updatePinHashDirectly(rs.getString("pin_hash"));
                users.add(user);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return users;
    }
}
