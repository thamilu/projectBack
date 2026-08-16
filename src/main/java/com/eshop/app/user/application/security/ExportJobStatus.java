package com.eshop.app.user.application.security;

import java.io.Serializable;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Context container representing the lifecycle status of an async export job. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportJobStatus implements Serializable {

    private static final long serialVersionUID = 1L;

    private String jobId;
    private String status; // PENDING, PROCESSING, COMPLETED, FAILED
    private String downloadUrl;
    private Long recordCount;
    private String errorMessage;
    private Instant createdAt;
    private Instant completedAt;
    private Long submittedBy;
}
