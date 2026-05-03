package com.eshop.app.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.function.Predicate;

public final class SlugUtils {

    private SlugUtils() {}

    public static String generateSlug(String input) {
        if (input == null) return null;
        String nowhitespace = input.trim().replaceAll("\\s+", "-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD)
            .replaceAll("[^\\p{ASCII}]", "");
        String slug = normalized.replaceAll("[^a-zA-Z0-9-]", "").toLowerCase(Locale.ROOT);
        slug = slug.replaceAll("-+", "-");
        if (slug.startsWith("-")) slug = slug.substring(1);
        if (slug.endsWith("-")) slug = slug.substring(0, slug.length()-1);
        return slug.isEmpty() ? "n-a" : slug;
    }

    public static String generateUniqueSlug(String base, Predicate<String> existsChecker) {
        String candidate = generateSlug(base);
        int suffix = 1;
        while (existsChecker.test(candidate)) {
            candidate = String.format("%s-%d", base.replaceAll("[^a-zA-Z0-9-]", "").toLowerCase(), suffix++);
        }
        return candidate;
    }
}
