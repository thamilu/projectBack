package com.eshop.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockByLocationDto {
    private Long warehouseId;
    private Long storeId;
    private Integer quantity;
}
