package com.eshop.app.catalog.domain.entity;





import lombok.Getter;

@Getter
public enum AttributeType {
    TEXT("Text"),
    NUMBER("Number"),
    SELECT("Select"),
    MULTISELECT("Multi-Select"),
    DATE("Date"),
    BOOLEAN("Yes/No");

    private final String displayName;

    AttributeType(String displayName) {
        this.displayName = displayName;
    }
}
