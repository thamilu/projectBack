package com.eshop.app.user.application.service;

import org.springframework.stereotype.Component;

/** Utility for masking sensitive data (PII) like email addresses and user IDs in logs. */
@Component
public class MaskingUtil {

    /**
     * Masks an email address by keeping the first and last character of the local part.
     *
     * @param email The raw email address
     * @return The masked email address
     */
    public String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        String[] parts = email.split("@");
        if (parts.length != 2) {
            return "***";
        }
        String local = parts[0];
        String masked =
                local.length() > 2
                        ? local.charAt(0) + "***" + local.charAt(local.length() - 1)
                        : "***";
        return masked + "@" + parts[1];
    }

    /**
     * Masks a user ID by keeping only the first and last 4 characters.
     *
     * @param userId The raw user ID
     * @return The masked user ID
     */
    public String maskUserId(String userId) {
        if (userId == null || userId.length() < 8) {
            return "***";
        }
        return userId.substring(0, 4) + "****" + userId.substring(userId.length() - 4);
    }
}
