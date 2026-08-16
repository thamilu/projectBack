package com.eshop.app.order.application.service.impl;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

/**
 * Thread-safe component for generating standardized order numbers.
 *
 * <p>{@code order_number} carries a DB-level unique constraint (see {@code Order}), so a
 * collision fails the whole checkout transaction with a 409 rather than silently duplicating —
 * the random suffix is sized to keep that probability negligible even under concurrent
 * high-traffic checkout (e.g. flash sales), where a 3-digit/second suffix would collide often
 * enough to surface as real customer-facing failures.
 */
@Component
public class OrderNumberGenerator {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final int RANDOM_SUFFIX_BOUND = 1_000_000;
    private final SecureRandom random = new SecureRandom();

    /**
     * Generates a standard order number in the format: ORD-YYYYMMDDHHMMSS-RANDOM.
     *
     * @return the unique order number
     */
    public String generateOrderNumber() {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        int rand = random.nextInt(RANDOM_SUFFIX_BOUND);
        return String.format("ORD-%s-%06d", timestamp, rand);
    }
}
