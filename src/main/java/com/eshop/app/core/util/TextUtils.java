package com.eshop.app.core.util;

/**
 * [HARDEN] Unified text manipulation utility.
 * Centralizes common string formatting and sanitization logic.
 */
public final class TextUtils {
    
    private TextUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Convert a string to Proper Case (e.g., "HELLO WORLD" -> "Hello World").
     * 
     * @param input The string to convert.
     * @return Proper cased string.
     */
    public static String toProperCase(String input) {
        if (input == null || input.isEmpty()) return input;
        
        StringBuilder sb = new StringBuilder();
        boolean nextTitleCase = true;
        
        for (char c : input.toLowerCase().toCharArray()) {
            if (Character.isSpaceChar(c)) {
                nextTitleCase = true;
            } else if (nextTitleCase) {
                c = Character.toTitleCase(c);
                nextTitleCase = false;
            }
            sb.append(c);
        }
        
        return sb.toString();
    }
}
