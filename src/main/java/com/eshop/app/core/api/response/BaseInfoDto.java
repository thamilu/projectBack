package com.eshop.app.core.api.response;





import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BaseInfoDto {
    private String description;
    private String shortDescription;
    private String sku;
    private String friendlyUrl;
    private Long brandId;
    private Long shopId;
}


