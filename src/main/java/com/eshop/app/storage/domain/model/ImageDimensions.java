package com.eshop.app.storage.domain.model;

public record ImageDimensions(int width, int height) {
    public static ImageDimensions unknown() {
        return new ImageDimensions(0, 0);
    }
}
