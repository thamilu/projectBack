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
public class ElectronicsAttributes implements CategoryAttributes {
    private final String type = "ELECTRONICS";
    private String brand;
    private String model;
    private String color;
    private String warranty;
    private Integer warrantyMonths;
    private List<String> features;

    @Override
    public String getType() { return type; }
}
