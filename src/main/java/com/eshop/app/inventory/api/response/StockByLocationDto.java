package com.eshop.app.inventory.api.response;





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
