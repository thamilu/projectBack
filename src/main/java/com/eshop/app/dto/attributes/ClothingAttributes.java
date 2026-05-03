package com.eshop.app.dto.attributes;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClothingAttributes implements CategoryAttributes {
    private final String type = "CLOTHING";
    private String brand;
    private String size;
    private List<String> availableSizes;
    private String color;
    private List<String> availableColors;

    @Override
    public String getType() { return type; }
}
