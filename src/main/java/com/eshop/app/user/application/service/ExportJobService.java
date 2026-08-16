package com.eshop.app.user.application.service;

import com.eshop.app.core.exception.base.BusinessException;
import com.eshop.app.core.exception.business.ResourceNotFoundException;
import com.eshop.app.user.api.request.ExportRequest;
import com.eshop.app.user.api.response.ExportStatusResponse;
import com.eshop.app.user.application.security.ExportJobStatus;
import com.eshop.app.user.application.service.impl.AsyncUserExportService;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Service orchestrating and tracking the lifecycle of asynchronous export requests. Uses
 * Redis-backed storage for multi-instance support and falls back to an in-memory Map if Redis is
 * unavailable.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExportJobService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final AsyncUserExportService asyncUserExportService;

    private final Map<String, ExportJobStatus> inMemoryFallback = new ConcurrentHashMap<>();
    private static final String REDIS_KEY_PREFIX = "eshop:export-job:";
    private static final Duration JOB_TTL = Duration.ofHours(24);

    /**
     * Submits a new export job task.
     *
     * @param request The export configuration
     * @param adminId The ID of the submitting admin
     * @param requestedByEmail The email of the admin to receive notifications
     * @return The unique job ID
     */
    public String submit(ExportRequest request, Long adminId, String requestedByEmail) {
        String jobId = UUID.randomUUID().toString();

        ExportJobStatus initialStatus =
                ExportJobStatus.builder()
                        .jobId(jobId)
                        .status("PENDING")
                        .createdAt(Instant.now())
                        .submittedBy(adminId)
                        .build();

        saveJob(jobId, initialStatus);

        // Asynchronously execute using AsyncUserExportService
        asyncUserExportService
                .exportUsersAsync(request, requestedByEmail)
                .thenAccept(
                        result -> {
                            ExportJobStatus current = getJob(jobId);
                            if (current != null) {
                                current.setStatus("COMPLETED");
                                current.setDownloadUrl(result.getDownloadUrl());
                                current.setRecordCount(result.getRecordCount());
                                current.setCompletedAt(Instant.now());
                                saveJob(jobId, current);
                            }
                        })
                .exceptionally(
                        ex -> {
                            ExportJobStatus current = getJob(jobId);
                            if (current != null) {
                                current.setStatus("FAILED");
                                current.setErrorMessage(ex.getMessage());
                                current.setCompletedAt(Instant.now());
                                saveJob(jobId, current);
                            }
                            return null;
                        });

        return jobId;
    }

    /**
     * Retrieves status of the export job. Enforces requester admin scoping.
     *
     * @param jobId The unique job ID
     * @param requestingAdminId The currently authenticated admin ID
     * @return The status details
     * @throws ResourceNotFoundException if job doesn't exist
     * @throws BusinessException if requesting admin does not own the job
     */
    public ExportStatusResponse getStatus(String jobId, Long requestingAdminId) {
        ExportJobStatus job = getJob(jobId);

        if (job == null) {
            throw new ResourceNotFoundException("Export job not found: " + jobId);
        }

        // Ownership check - prevent cross-admin job snooping
        if (!job.getSubmittedBy().equals(requestingAdminId)) {
            log.warn(
                    "Admin {} attempted to access export job {} owned by {}",
                    requestingAdminId,
                    jobId,
                    job.getSubmittedBy());
            throw new BusinessException(
                    "Export job not found", // Intentionally vague for security
                    "EXPORT_NOT_FOUND",
                    HttpStatus.NOT_FOUND);
        }

        return ExportStatusResponse.builder()
                .jobId(job.getJobId())
                .status(job.getStatus())
                .downloadUrl(job.getDownloadUrl())
                .recordCount(job.getRecordCount())
                .errorMessage(job.getErrorMessage())
                .createdAt(job.getCreatedAt())
                .completedAt(job.getCompletedAt())
                .build();
    }

    private void saveJob(String jobId, ExportJobStatus status) {
        try {
            String redisKey = REDIS_KEY_PREFIX + jobId;
            redisTemplate.opsForValue().set(redisKey, status, JOB_TTL);
        } catch (Exception e) {
            log.warn(
                    "Redis write failed for export job {}, falling back to in-memory: {}",
                    jobId,
                    e.getMessage());
            inMemoryFallback.put(jobId, status);
        }
    }

    private ExportJobStatus getJob(String jobId) {
        try {
            String redisKey = REDIS_KEY_PREFIX + jobId;
            ExportJobStatus status = (ExportJobStatus) redisTemplate.opsForValue().get(redisKey);
            if (status != null) {
                return status;
            }
        } catch (Exception e) {
            log.warn(
                    "Redis read failed for export job {}, falling back to in-memory: {}",
                    jobId,
                    e.getMessage());
        }
        return inMemoryFallback.get(jobId);
    }
}
