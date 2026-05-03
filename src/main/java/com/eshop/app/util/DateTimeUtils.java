package com.eshop.app.util;

import java.time.LocalDateTime;

/**
 * Centralised date-time helper utilities.
 * <p>
 * Eliminates repeated inline {@code LocalDateTime.now().withHour(0)...} calculations
 * scattered across service implementations (DRY principle).
 */
public final class DateTimeUtils {

    private DateTimeUtils() {
        // utility class – no instantiation
    }

    /**
     * Beginning of today (00:00:00.000).
     */
    public static LocalDateTime startOfDay() {
        return LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
    }

    /**
     * Beginning of the current calendar month (1st day, 00:00:00.000).
     */
    public static LocalDateTime startOfMonth() {
        return startOfDay().withDayOfMonth(1);
    }

    /**
     * Exactly one week ago from now (rolling 7-day window).
     */
    public static LocalDateTime startOfWeek() {
        return LocalDateTime.now().minusWeeks(1);
    }
}
