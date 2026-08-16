package com.eshop.app.user.domain.event;

import com.eshop.app.core.exception.business.DomainValidationException;
import java.net.InetAddress;

/**
 * Shared validator for domain events within the package.
 *
 * <p>Ensures consistent validation rules and adheres to privacy rules by not exposing invalid
 * parameter values in error messages.
 */
public final class EventValidator {

    private EventValidator() {
        throw new UnsupportedOperationException("Utility class — do not instantiate");
    }

    public static String requireNonBlank(String value, String field, String message) {
        if (value == null || value.isBlank()) {
            throw new DomainValidationException(field, message);
        }
        return value.trim();
    }

    public static <T> T requireNonNull(T value, String field, String message) {
        if (value == null) {
            throw new DomainValidationException(field, message);
        }
        return value;
    }

    public static Long requirePositiveLong(Long value, String field, String message) {
        if (value == null || value <= 0) {
            throw new DomainValidationException(field, message);
        }
        return value;
    }

    public static int requirePositiveInt(int value, String field, String message) {
        if (value <= 0) {
            throw new DomainValidationException(field, message);
        }
        return value;
    }

    public static String validateIpAddress(String ipAddress, String requiredMessage) {
        if (ipAddress == null || ipAddress.isBlank()) {
            throw new DomainValidationException("ipAddress", requiredMessage);
        }

        String trimmed = ipAddress.trim();

        if (!isValidIpAddress(trimmed)) {
            throw new DomainValidationException(
                    "ipAddress", "IP address must be a valid IPv4 or IPv6 address");
        }

        return trimmed;
    }

    private static boolean isValidIpAddress(String ip) {
        if (ip == null || ip.isBlank()) {
            return false;
        }

        // Strip brackets if bracketed IPv6
        if (ip.startsWith("[") && ip.endsWith("]")) {
            ip = ip.substring(1, ip.length() - 1);
        }

        // If it contains colons, it must be an IPv6 address.
        // We verify by parsing it with InetAddress.getByName.
        if (ip.contains(":")) {
            try {
                InetAddress address = InetAddress.getByName(ip);
                return address instanceof java.net.Inet6Address;
            } catch (Exception e) {
                return false;
            }
        }

        // Otherwise, it must be an IPv4 address.
        return isValidIPv4(ip);
    }

    private static boolean isValidIPv4(String ip) {
        String[] octets = ip.split("\\.", -1);
        if (octets.length != 4) {
            return false;
        }
        for (String octet : octets) {
            if (octet.isEmpty() || octet.length() > 3) {
                return false;
            }
            // Check all characters are digits
            for (int i = 0; i < octet.length(); i++) {
                char c = octet.charAt(i);
                if (c < '0' || c > '9') {
                    return false;
                }
            }
            try {
                int val = Integer.parseInt(octet);
                if (val < 0 || val > 255) {
                    return false;
                }
                // Avoid leading zeros if length > 1 (e.g. "192.168.001.001" is ambiguous)
                if (octet.length() > 1 && octet.startsWith("0")) {
                    return false;
                }
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return true;
    }

    /**
     * Normalizes an optional string field. Returns null for null or blank input; trimmed value
     * otherwise.
     *
     * @param value candidate value (nullable)
     * @return trimmed value, or null if absent or blank
     */
    public static String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
