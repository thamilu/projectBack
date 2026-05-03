package com.eshop.app.dto.response;

import java.io.Serializable;

/**
 * DTO for Language entity - Safe for Redis caching
 * Contains only primitive Java types, no Locale or JPA entities
 */
public record LanguageDTO(
    Long id,
    String code,
    String name,
    String locale,
    Boolean rtl,
    Boolean isDefault,
    Boolean active,
    Integer sortOrder,
    String flagIcon
) implements Serializable {
}
