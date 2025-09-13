package com.banking.services;

import com.banking.config.DbConnection;
import com.banking.models.User;
import com.banking.repositories.UserRepository;
import com.banking.utils.CryptoUtil;
import com.banking.utils.ValidationUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AuthService - handles register, login, logout, pin changes and simple session management.
 *
 * Notes:
 * - Relies on UserRepository for reads and for updating failed-login/locked flags.
 * - Directly updates pin hash in DB via JDBC (updatePinHashInDb) to avoid adding that method to UserRepository.
 */
public class AuthService {

    private final UserRepository userRepository;
    private final Map<UUID, User> sessions = new ConcurrentHashMap<>(); // sessionId -> User
    private final int maxFailedAttempts;

    public AuthService(UserRepository userRepository) {
        this(userRepository, 3); // default max attempts = 3
    }

    public AuthService(UserRepository userRepository, int maxFailedAttempts) {
        this.userRepository = userRepository;
        this.maxFailedAttempts = Math.max(1, maxFailedAttempts);
    }

    /**
     * Attempt to log a user in.
     * On success returns a new sessionId (UUID) that represents the logged in session.
     *
     * @param username plain username
     * @param rawPin   plain PIN string (will be verified against stored hash)
     * @return sessionId UUID for the session
     * @throws AuthException on invalid credentials / locked account
     */
    public UUID login(String username, String rawPin) throws AuthException {
        if (username == null || username.isBlank() || rawPin == null || rawPin.isBlank()) {
            throw new AuthException("Username and PIN must be provided");
        }

        User user = userRepository.findByUsername(username);
        if (user == null) {
            // do not reveal whether username exists
            throw new AuthException("Invalid username or PIN");
        }

        if (user.isLocked()) {
            throw new UserLockedException("Account is locked. Contact administrator.");
        }

        boolean verified = CryptoUtil.checkPassword(rawPin, user.getPinHash());
        if (verified) {
            // successful login -> reset failed attempts in DB and create session
            userRepository.updateFailedLoginAttempts(user.getUserId(), 0);
            UUID sessionId = UUID.randomUUID();
            sessions.put(sessionId, user);
            return sessionId;
        } else {
            // failed login -> increment counter
            int attempts = user.getFailedLoginAttempts() + 1;
            userRepository.updateFailedLoginAttempts(user.getUserId(), attempts);

            if (attempts >= maxFailedAttempts) {
                // lock user in DB
                userRepository.setLocked(user.getUserId(), true);
                throw new UserLockedException("Account locked due to too many failed login attempts");
            } else {
                int remaining = maxFailedAttempts - attempts;
                throw new AuthException("Invalid username or PIN. Attempts remaining: " + remaining);
            }
        }
    }

    /**
     * Log out a session.
     *
     * @param sessionId the session id returned by login
     */
    public void logout(UUID sessionId) {
        if (sessionId != null) {
            sessions.remove(sessionId);
        }
    }

    /**
     * Returns true if sessionId maps to an authenticated user.
     */
    public boolean isAuthenticated(UUID sessionId) {
        return sessionId != null && sessions.containsKey(sessionId);
    }

    /**
     * Return the User object for a session, or null if not authenticated.
     */
    public User getUserForSession(UUID sessionId) {
        return sessions.get(sessionId);
    }

    /**
     * Register a new user. Throws AuthException if username already exists or validation fails.
     *
     * @param username plain username
     * @param rawPin   plain pin
     * @return created User
     */
    public User register(String username, String rawPin) throws AuthException {
        // validate input (reuse existing ValidationUtil rules)
        ValidationUtil.validateUserName(username);
        ValidationUtil.validatePinFormat(rawPin);

        // check for existing username
        if (userRepository.findByUsername(username) != null) {
            throw new AuthException("Username already exists");
        }

        User newUser = new User(username, rawPin);
        userRepository.save(newUser);
        return newUser;
    }

    /**
     * Change the PIN for the currently authenticated user.
     * This updates the User object in memory (session) and also persists the new hash in the DB.
     *
     * @param sessionId session of the logged-in user
     * @param newPin    new raw pin
     * @throws AuthException when not authenticated or validation fails
     */
    public void changePin(UUID sessionId, String newPin) throws AuthException {
        if (!isAuthenticated(sessionId)) {
            throw new AuthException("Not authenticated");
        }
        ValidationUtil.validatePinFormat(newPin);

        User user = sessions.get(sessionId);
        if (user == null) throw new AuthException("Invalid session");

        // update in-memory user
        user.updatePin(newPin);

        // persist new hash to DB directly
        updatePinHashInDb(user.getUserId(), user.getPinHash());
    }

    /**
     * Unlock a user (admin operation).
     *
     * @param userId id of user to unlock
     */
    public void unlockUser(UUID userId) {
        userRepository.setLocked(userId, false);
        userRepository.updateFailedLoginAttempts(userId, 0);
    }

    /**
     * Helper: update pin hash directly in DB. We do this here to avoid changing the existing UserRepository
     * (but ideally you'd add an updatePinHash method to the repository).
     */
    private void updatePinHashInDb(UUID userId, String newHash) throws AuthException {
        String sql = "UPDATE users SET pin_hash = ? WHERE user_id = ?";

        try (Connection conn = DbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newHash);
            pstmt.setObject(2, userId);
            int updated = pstmt.executeUpdate();
            if (updated == 0) {
                throw new AuthException("Failed to persist new PIN hash");
            }
        } catch (SQLException e) {
            throw new AuthException("Failed to persist new PIN hash: " + e.getMessage(), e);
        }
    }

    // ---------------------- Exceptions ----------------------

    public static class AuthException extends Exception {
        public AuthException(String message) {
            super(message);
        }

        public AuthException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class UserLockedException extends AuthException {
        public UserLockedException(String message) {
            super(message);
        }
    }
}

