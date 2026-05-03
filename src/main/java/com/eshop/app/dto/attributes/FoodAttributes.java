package com.eshop.app.dto.attributes;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodAttributes implements CategoryAttributes {
    private final String type = "FOOD";
    private String brand;
    private String foodType;
    private Double weight;
    private String weightUnit;
    private List<String> ingredients;
    private List<String> allergens;
    private LocalDate expiryDate;

    @Override
    public String getType() { return type; }
}
