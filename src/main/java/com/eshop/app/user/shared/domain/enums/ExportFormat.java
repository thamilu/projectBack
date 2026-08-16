package com.eshop.app.user.shared.domain.enums;

import org.springframework.http.MediaType;

/**
 * Unified export format for user-data export operations.
 *
 * <p>Each constant maps a logical export format to its file extension
 * and the corresponding HTTP {@link MediaType}, ensuring consistent
 * {@code Content-Type} and {@code Content-Disposition} handling across
 * export API responses.</p>
 *
 * <p><strong>Architecture note:</strong> This enum currently depends on
 * {@link org.springframework.http.MediaType} (a Spring Web type) despite
 * residing in the domain layer. Consider separating domain representation from
 * web media types if exporting outside HTTP delivery contexts.</p>
 */
public enum ExportFormat {

    /** Portable Document Format export ({@code .pdf}, {@code application/pdf}). */
    PDF("pdf", MediaType.APPLICATION_PDF),

    /** Microsoft Excel (OOXML) export ({@code .xlsx}, spreadsheetml media type). */
    EXCEL("xlsx", MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")),

    /** Comma-separated values export ({@code .csv}, {@code text/csv}). */
    CSV("csv", MediaType.parseMediaType("text/csv")),

    /** JSON export ({@code .json}, {@code application/json}). */
    JSON("json", MediaType.APPLICATION_JSON);

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

    /**
     * Safely resolves an {@link ExportFormat} from a string extension (case-insensitive).
     *
     * @param extension file extension (e.g. "pdf", "xlsx", "csv", "json")
     * @return matching {@link ExportFormat}, or {@code null} if unknown or blank
     */
    public static ExportFormat fromExtension(String extension) {
        if (extension == null || extension.isBlank()) {
            return null;
        }
        String clean = extension.trim().toLowerCase();
        for (ExportFormat format : values()) {
            if (format.extension.equals(clean)) {
                return format;
            }
        }
        return null;
    }
}

