package com.eshop.app.seed.model;

/**
 * Immutable record representing tag seed data.
 */
public record TagData(
    String name
) {
    public static TagData of(String name) {
        return new TagData(name);
    }
}
