package com.eshop.app.core.util;

import com.eshop.app.core.util.SlugUtils;

import java.text.Normalizer;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * [HARDEN] Unified slug generation utility.
 * Ensures URL-friendly identifiers are generated consistently across the platform.
 */
public final class SlugUtils {
    
    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    private SlugUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static String generateSlug(String input) {
        if (input == null || input.isEmpty()) return "";
        String nowhitespace = WHITESPACE.matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");
        return slug.toLowerCase(Locale.ENGLISH).replaceAll("-{2,}", "-").replaceAll("^-|-$", "");
    }

    public static String generateUniqueSlug(String baseSlug, Predicate<String> existsCheck) {
        String slug = baseSlug;
        int count = 1;
        while (existsCheck.test(slug)) {
            slug = baseSlug + "-" + count++;
        }
        return slug;
    }
}

