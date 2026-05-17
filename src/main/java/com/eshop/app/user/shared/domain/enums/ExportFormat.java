package com.eshop.app.user.shared.domain.enums;

import org.springframework.http.MediaType;

/**
 * [HARDEN] Unified export format enum.
 * Supports file extension and media type mapping for API responses.
 */
public enum ExportFormat {
    PDF("pdf", MediaType.APPLICATION_PDF),
    EXCEL("xlsx", MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")),
    CSV("csv", MediaType.parseMediaType("text/csv")),
    JSON("json", MediaType.APPLICATION_JSON);

    private final String extension;
    private final MediaType mediaType;

    ExportFormat(String extension, MediaType mediaType) {
        this.extension = extension;
        this.mediaType = mediaType;
    }

    public String getExtension() { return extension; }
    public MediaType getMediaType() { return mediaType; }
}
