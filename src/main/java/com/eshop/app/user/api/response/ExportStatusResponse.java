package com.eshop.app.user.api.response;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Return response holding standard details of the export job lifecycle status. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportStatusResponse {
    private String jobId;
    private String status; // PENDING, PROCESSING, COMPLETED, FAILED
    private String downloadUrl;
    private Long recordCount;
    private String errorMessage;
    private Instant createdAt;
    private Instant completedAt;
}
