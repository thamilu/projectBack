package com.eshop.app.core.kernel;

/** Utility class for domain-level name operations. */
public final class NameUtils {

    private NameUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Safe and trimmed formatting of full names from first and last name components.
     *
     * @param firstName first name component (nullable)
     * @param lastName last name component (nullable)
     * @return formatted full name (trimmed)
     */
    public static String buildFullName(String firstName, String lastName) {
        String first = firstName != null ? firstName.trim() : "";
        String last = lastName != null ? lastName.trim() : "";
        if (first.isEmpty()) return last;
        if (last.isEmpty()) return first;
        return first + " " + last;
    }
}
