package com.eshop.app.util;

import org.springframework.util.StringUtils;

/**
 * Shared utility class for search keyword sanitization.
 *
 * <p>Centralises all search sanitization logic to enforce DRY principle across
 * service and controller layers. Previously duplicated in:
 * <ul>
 *   <li>BrandServiceImpl</li>
 *   <li>CategoryServiceImpl</li>
 *   <li>CategoryController</li>
 *   <li>UserController</li>
 * </ul>
 *
 * @since 2.1
 */
public final class SearchUtils {

    private SearchUtils() {
        throw new UnsupportedOperationException("Cannot instantiate utility class");
    }

    /**
     * Service-level sanitizer: strips SQL wildcard characters and normalizes whitespace.
     * Safe for use in LIKE queries or full-text search.
     *
     * @param keyword raw search input
     * @return sanitized keyword, or empty string if blank
     */
    public static String sanitize(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return "";
        }
        return keyword.trim()
                .replaceAll("[%_]", "")
                .replaceAll("\\s+", " ");
    }

    /**
     * Controller / strict sanitizer: additionally strips all special characters,
     * keeping only alphanumeric, spaces, and hyphens. Truncated to 100 chars max.
     *
     * @param keyword raw search input
     * @return strictly sanitized keyword, or empty string if blank
     */
    public static String sanitizeStrict(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return "";
        }
        String trimmed = keyword.trim();
        String cleaned = trimmed.replaceAll("[^a-zA-Z0-9\\s-]", "");
        return cleaned.substring(0, Math.min(cleaned.length(), 100));
    }
}
