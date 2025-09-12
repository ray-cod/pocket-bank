package com.banking.utils;

import org.mindrot.jbcrypt.BCrypt;

public class CryptoUtil {

    // Hash Password
    public static String hashPassword(String pin){
        String salt = BCrypt.gensalt(12);
        return BCrypt.hashpw(pin, salt);
    }

    // Verify Password
    public static boolean checkPassword(String plainPin, String hashedPin) {
        return BCrypt.checkpw(plainPin, hashedPin);
    }
}
