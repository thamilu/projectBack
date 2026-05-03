package com.eshop.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryDto {
    private Integer totalStock;
    private Integer lowStockThreshold;
    private Boolean trackInventory;
    private Boolean allowBackorder;
    private List<StockByLocationDto> stockByLocation;
}
