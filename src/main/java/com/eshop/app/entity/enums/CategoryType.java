package com.eshop.app.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CategoryType {
    ELECTRONICS("Electronics"),
    SMARTPHONE("Smartphone"),
    LAPTOP("Laptop"),
    CLOTHING("Clothing"),
    FOOTWEAR("Footwear"),
    FOOD("Food"),
    GROCERY("Grocery"),
    FURNITURE("Furniture"),
    BEAUTY("Beauty"),
    BOOK("Book"),
    JEWELRY("Jewelry"),
    OTHER("Other");

    private final String displayName;
}
