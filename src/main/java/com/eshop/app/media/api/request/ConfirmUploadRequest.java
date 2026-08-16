package com.eshop.app.media.api.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for confirming a successful upload.
 *
 * <p>After the frontend uploads directly to R2 using the presigned URL, it calls the confirm
 * endpoint with the upload ID and image metadata.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmUploadRequest {

    /** The upload ID returned from the presigned URL request. */
    @NotNull(message = "Upload ID is required")
    private UUID uploadId;

    /** Image width in pixels (extracted by frontend after upload). */
    @Min(value = 1, message = "Width must be at least 1 pixel")
    private Integer width;

    /** Image height in pixels (extracted by frontend after upload). */
    @Min(value = 1, message = "Height must be at least 1 pixel")
    private Integer height;

    /** Actual file size after upload (for verification). */
    @Min(value = 1, message = "File size must be at least 1 byte")
    private Long fileSize;
}
