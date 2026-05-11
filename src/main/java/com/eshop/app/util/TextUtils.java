package com.eshop.app.util;

import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Enterprise text manipulation utilities.
 */
public final class TextUtils {

    private TextUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Converts a string to Proper Case (Title Case).
     * Example: "KARNATAKA" -> "Karnataka", "TAMIL NADU" -> "Tamil Nadu".
     *
     * @param input Raw string
     * @return Proper Case string, or empty if input is blank
     */
    public static String toProperCase(String input) {
        if (!StringUtils.hasText(input)) {
            return "";
        }

        return Arrays.stream(input.trim().split("\\s+"))
                .map(word -> {
                    if (word.length() <= 1) return word.toUpperCase();
                    return word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase();
                })
                .collect(Collectors.joining(" "));
    }
}
