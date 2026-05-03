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
public class LaptopAttributes implements CategoryAttributes {
    private final String type = "LAPTOP";
    private String brand;
    private String model;
    private String processor;
    private String ram;
    private String storage;
    private List<String> ports;

    @Override
    public String getType() { return type; }
}
