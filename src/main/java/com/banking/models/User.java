package com.banking.models;

import com.banking.utils.CryptoUtil;
import com.banking.utils.ValidationUtil;
import com.banking.utils.enums.UserRole;

import java.util.UUID;

public class User {

    private final UUID userId = UUID.randomUUID();
    private String userName;
    private String pinHash;
    private boolean locked = false;
    private int failedLoginAttempts = 0;
    private UserRole role = UserRole.USER;

    // Constructor
    public User(String userName, String pin) {
        ValidationUtil.validateUserName(userName);
        ValidationUtil.validatePinFormat(pin);

        this.userName = userName;
        this.pinHash = CryptoUtil.hashPassword(pin);
    }

    // Getters & Setters
    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        ValidationUtil.validateUserName(userName);
        this.userName = userName;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getPinHash() {
        return pinHash;
    }

    public void updatePin(String rawPin) {
        ValidationUtil.validatePinFormat(rawPin);
        this.pinHash = CryptoUtil.hashPassword(rawPin);
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public void setFailedLoginAttempts(int failedLoginAttempts) {
        this.failedLoginAttempts = failedLoginAttempts;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    // Helper
    public void updatePinHashDirectly(String hashedPin) {
        this.pinHash = hashedPin;
    }

    // toString
    @Override
    public String toString() {
        return "User{" +
                "userId=" + userId +
                ", userName='" + userName + '\'' +
                ", locked=" + locked +
                ", failedLoginAttempts=" + failedLoginAttempts +
                ", role=" + role +
                '}';
    }
}
