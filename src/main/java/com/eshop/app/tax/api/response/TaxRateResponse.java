package com.eshop.app.tax.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxRateResponse {
    private Long id;
    private String name;
    private BigDecimal rate;
    private String type;
    private Long geoZoneId;
    private Integer priority;
    private Boolean compound;
    private Boolean active;
}
