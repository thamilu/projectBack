package com.eshop.app.enums;

import org.springframework.http.MediaType;

public enum ExportFormat {
    CSV("csv", MediaType.TEXT_PLAIN),
    EXCEL("xlsx", MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

    private final String extension;
    private final MediaType mediaType;

    ExportFormat(String extension, MediaType mediaType) {
        this.extension = extension;
        this.mediaType = mediaType;
    }

    public String getExtension() {
        return extension;
    }

    public MediaType getMediaType() {
        return mediaType;
    }
}
