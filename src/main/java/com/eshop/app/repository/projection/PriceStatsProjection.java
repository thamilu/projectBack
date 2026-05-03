package com.eshop.app.repository.projection;

import java.math.BigDecimal;

public interface PriceStatsProjection {
    BigDecimal getAveragePrice();
    BigDecimal getMinPrice();
    BigDecimal getMaxPrice();
}
