package com.eshop.app.media.api.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for generating a presigned upload URL.
 *
 * <p>The frontend sends this before uploading — the backend validates the request, creates a
 * PENDING media asset, and returns a presigned URL for direct-to-R2 upload.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestUploadUrlRequest {

    /** Product to associate the image with. Nullable — allows uploading before product creation. */
    private Long productId;

    /** Original filename from the user's device. */
    @NotBlank(message = "Filename is required")
    @Size(max = 255, message = "Filename cannot exceed 255 characters")
    @Pattern(regexp = "^[a-zA-Z0-9._\\-\\s]+$", message = "Filename contains invalid characters")
    private String filename;

    /** MIME type of the file to upload. */
    @NotBlank(message = "Content type is required")
    @Pattern(
            regexp = "^image/(jpeg|png|webp)$",
            message = "Only JPEG, PNG, and WebP images are allowed")
    private String contentType;

    /** File size in bytes (for pre-upload validation). */
    @NotNull(message = "File size is required")
    @Min(value = 1, message = "File size must be at least 1 byte")
    @Max(value = 5242880, message = "File size cannot exceed 5MB")
    private Long fileSize;
}
