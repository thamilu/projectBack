package com.eshop.app.pricing.domain.repository;





import java.math.BigDecimal;

public interface PriceStatsProjection {
    BigDecimal getAveragePrice();
    BigDecimal getMinPrice();
    BigDecimal getMaxPrice();
}
