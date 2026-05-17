package com.eshop.app.core.util;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.time.DayOfWeek;

/**
 * [HARDEN] Unified date and time utility.
 * Standardizes common date calculations for reporting and analytics.
 */
public final class DateTimeUtils {
    
    private DateTimeUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static LocalDateTime startOfDay() {
        return LocalDateTime.now().with(LocalTime.MIN);
    }

    public static LocalDateTime startOfWeek() {
        return LocalDateTime.now().with(DayOfWeek.MONDAY).with(LocalTime.MIN);
    }

    public static LocalDateTime startOfMonth() {
        return LocalDateTime.now().with(TemporalAdjusters.firstDayOfMonth()).with(LocalTime.MIN);
    }
    
    public static LocalDateTime startOfYear() {
        return LocalDateTime.now().with(TemporalAdjusters.firstDayOfYear()).with(LocalTime.MIN);
    }
}
