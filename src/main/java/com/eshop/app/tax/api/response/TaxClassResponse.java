package com.eshop.app.tax.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxClassResponse {
    private Long id;
    private String name;
    private String description;
    private Boolean active;
}
