package com.banking.utils;

public class ValidationUtil {

    private static final int MIN_USERNAME_LENGTH = 3;
    private static final int MAX_USERNAME_LENGTH = 30;
    private static final int MIN_PIN_LENGTH = 4;
    private static final int MAX_PIN_LENGTH = 20;

    // Validate username
    public static void validateUserName(String userName) {
        if (userName == null || userName.isBlank()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }
        if (userName.length() < MIN_USERNAME_LENGTH || userName.length() > MAX_USERNAME_LENGTH) {
            throw new IllegalArgumentException("Username must be between " + MIN_USERNAME_LENGTH +
                    " and " + MAX_USERNAME_LENGTH + " characters long");
        }
        if (!userName.matches("^[a-zA-Z0-9_ ]+$")) {
            throw new IllegalArgumentException("Username can only contain letters, numbers, and underscores");
        }
    }

    // Validate PIN before hashing
    public static void validatePinFormat(String rawPin) {
        if (rawPin == null || rawPin.isBlank()) {
            throw new IllegalArgumentException("PIN cannot be empty");
        }
        if (rawPin.length() < MIN_PIN_LENGTH || rawPin.length() > MAX_PIN_LENGTH) {
            throw new IllegalArgumentException("PIN must be between " + MIN_PIN_LENGTH +
                    " and " + MAX_PIN_LENGTH + " digits long");
        }
        if (!rawPin.matches("\\d+")) {
            throw new IllegalArgumentException("PIN must contain only digits");
        }
    }
}
