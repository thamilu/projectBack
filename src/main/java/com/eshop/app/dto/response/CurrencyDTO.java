package com.eshop.app.dto.response;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * DTO for Currency entity - Safe for Redis caching
 * Contains only primitive Java types, no JPA entities
 */
public record CurrencyDTO(
    Long id,
    String code,
    String name,
    String symbol,
    String symbolPosition,
    Integer decimalPlaces,
    BigDecimal exchangeRate,
    Boolean isDefault,
    Boolean active
) implements Serializable {
}
