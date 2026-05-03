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
public class FootwearAttributes implements CategoryAttributes {
    private final String type = "FOOTWEAR";
    private String brand;
    private String size;
    private List<String> availableSizes;
    private String color;

    @Override
    public String getType() { return type; }
}
