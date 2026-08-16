package com.eshop.app.user.infrastructure.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class to mask sensitive personal identifiable information (PII) in logs to maintain
 * compliance with GDPR and other data privacy regulations.
 */
public final class LogMaskingUtils {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("(^[^@]{1,3})[^@]*(@.*)");

    private LogMaskingUtils() {}

    /**
     * Masks an email address to hide details. E.g. "admin@eshop.com" -> "adm***@eshop.com"
     *
     * @param email The raw email address to mask
     * @return The masked email address, or "[null]" if null
     */
    public static String maskEmail(String email) {
        if (email == null) {
            return "[null]";
        }
        Matcher m = EMAIL_PATTERN.matcher(email);
        return m.matches() ? m.group(1) + "***" + m.group(2) : "[masked]";
    }

    /**
     * Masks a unique ID to hide details. E.g. "keycloak-12345-6789" -> "keyc****6789"
     *
     * @param id The raw ID string to mask
     * @return The masked ID string, or "[null]" if null
     */
    public static String maskId(String id) {
        if (id == null) {
            return "[null]";
        }
        return id.length() > 8
                ? id.substring(0, 4) + "****" + id.substring(id.length() - 4)
                : "****";
    }
}
