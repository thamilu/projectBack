package com.eshop.app.mapper.common;

import org.mapstruct.Named;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;

/**
 * Common mapping utility methods shared across all mappers.
 */
@Component
public class CommonMappingConfig {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final ZoneId DEFAULT_ZONE = ZoneId.systemDefault();

    @Named("trimString")
    public String trimString(String value) {
        return value != null ? value.trim() : null;
    }

    @Named("toLowerCase")
    public String toLowerCase(String value) {
        return value != null ? value.toLowerCase().trim() : null;
    }

    @Named("toUpperCase")
    public String toUpperCase(String value) {
        return value != null ? value.toUpperCase().trim() : null;
    }

    @Named("nullIfBlank")
    public String nullIfBlank(String value) {
        return (value != null && !value.isBlank()) ? value.trim() : null;
    }

    @Named("instantToLocalDateTime")
    public LocalDateTime instantToLocalDateTime(Instant instant) {
        return instant != null
                ? LocalDateTime.ofInstant(instant, DEFAULT_ZONE)
                : null;
    }

    @Named("localDateTimeToInstant")
    public Instant localDateTimeToInstant(LocalDateTime dateTime) {
        return dateTime != null
                ? dateTime.atZone(DEFAULT_ZONE).toInstant()
                : null;
    }

    @Named("instantToString")
    public String instantToString(Instant instant) {
        if (instant == null) return null;
        return DATE_FORMATTER.format(instant.atZone(DEFAULT_ZONE));
    }

    @Named("epochToInstant")
    public Instant epochToInstant(Long epoch) {
        return epoch != null ? Instant.ofEpochMilli(epoch) : null;
    }

    @Named("instantToEpoch")
    public Long instantToEpoch(Instant instant) {
        return instant != null ? instant.toEpochMilli() : null;
    }

    @Named("roundToTwoDecimals")
    public BigDecimal roundToTwoDecimals(BigDecimal value) {
        return value != null
                ? value.setScale(2, RoundingMode.HALF_UP)
                : null;
    }

    @Named("percentageToDecimal")
    public BigDecimal percentageToDecimal(BigDecimal percentage) {
        return percentage != null
                ? percentage.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
                : null;
    }

    @Named("calculateDiscountPercentage")
    public BigDecimal calculateDiscountPercentage(BigDecimal original, BigDecimal discounted) {
        if (original == null || discounted == null || original.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return original.subtract(discounted)
                .divide(original, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Named("collectionSize")
    public Integer collectionSize(Collection<?> collection) {
        return collection != null ? collection.size() : 0;
    }

    @Named("isNotEmpty")
    public Boolean isNotEmpty(Collection<?> collection) {
        return collection != null && !collection.isEmpty();
    }

    @Named("booleanToString")
    public String booleanToString(Boolean value) {
        if (value == null) return "unknown";
        return value ? "yes" : "no";
    }

    @Named("activeToStatus")
    public String activeToStatus(Boolean active) {
        if (active == null) return "UNKNOWN";
        return active ? "ACTIVE" : "INACTIVE";
    }
}
