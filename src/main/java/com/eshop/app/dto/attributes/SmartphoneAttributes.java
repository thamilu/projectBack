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
public class SmartphoneAttributes implements CategoryAttributes {
    private final String type = "SMARTPHONE";
    private String brand;
    private String model;
    private String color;
    private List<String> availableColors;
    private String storage;
    private List<String> availableStorage;
    private String screenSize;
    private String processor;

    @Override
    public String getType() { return type; }
}
