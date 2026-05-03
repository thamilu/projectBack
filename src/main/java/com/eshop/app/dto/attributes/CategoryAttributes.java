package com.eshop.app.dto.attributes;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = ElectronicsAttributes.class, name = "ELECTRONICS"),
        @JsonSubTypes.Type(value = SmartphoneAttributes.class, name = "SMARTPHONE"),
        @JsonSubTypes.Type(value = LaptopAttributes.class, name = "LAPTOP"),
        @JsonSubTypes.Type(value = ClothingAttributes.class, name = "CLOTHING"),
        @JsonSubTypes.Type(value = FootwearAttributes.class, name = "FOOTWEAR"),
        @JsonSubTypes.Type(value = FoodAttributes.class, name = "FOOD")
})
public interface CategoryAttributes {
    String getType();
}
