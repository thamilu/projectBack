package com.eshop.app.media.api.response;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Response DTO returned after requesting a presigned upload URL.
 *
 * <p>Contains the presigned URL for direct-to-R2 upload, along with the upload ID needed for the
 * confirm step.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadUrlResponse {

    /** Upload ID — use this to confirm the upload after completing it. */
    private UUID uploadId;

    /**
     * Presigned PUT URL for uploading directly to R2. Expires after the configured duration
     * (default: 15 minutes).
     */
    private String uploadUrl;

    /** The storage key where the file will be stored. Frontend can use this for reference. */
    private String storageKey;

    /** When the presigned URL expires (ISO 8601). */
    private LocalDateTime expiresAt;
}
