package com.eshop.app.user.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.eshop.app.core.exception.base.BusinessException;
import com.eshop.app.user.api.request.ExportRequest;
import com.eshop.app.user.api.response.ExportResult;
import com.eshop.app.user.api.response.ExportStatusResponse;
import com.eshop.app.user.application.security.ExportJobStatus;
import com.eshop.app.user.application.service.impl.AsyncUserExportService;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class ExportJobServiceTest {

    @Mock private RedisTemplate<String, Object> redisTemplate;

    @Mock private ValueOperations<String, Object> valueOperations;

    @Mock private AsyncUserExportService asyncUserExportService;

    @InjectMocks private ExportJobService exportJobService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void testSubmit_Success() {
        ExportRequest request = new ExportRequest();
        request.setFormat(com.eshop.app.user.shared.domain.enums.ExportFormat.CSV);

        ExportResult result =
                ExportResult.builder()
                        .downloadUrl("http://s3.amazonaws.com/test.csv")
                        .recordCount(50L)
                        .build();

        when(asyncUserExportService.exportUsersAsync(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(result));

        String jobId = exportJobService.submit(request, 100L, "admin@example.com");

        assertNotNull(jobId);
        verify(asyncUserExportService).exportUsersAsync(any(), eq("admin@example.com"));
    }

    @Test
    void testGetStatus_OwnerMismatch_ThrowsException() {
        ExportJobStatus status =
                ExportJobStatus.builder()
                        .jobId("job-123")
                        .status("COMPLETED")
                        .submittedBy(100L)
                        .build();

        when(valueOperations.get("eshop:export-job:job-123")).thenReturn(status);

        BusinessException ex =
                assertThrows(
                        BusinessException.class, () -> exportJobService.getStatus("job-123", 200L));

        assertEquals("Export job not found", ex.getMessage());
        assertEquals("EXPORT_NOT_FOUND", ex.getErrorCode());
    }

    @Test
    void testGetStatus_OwnerMatch_ReturnsStatus() {
        ExportJobStatus status =
                ExportJobStatus.builder()
                        .jobId("job-123")
                        .status("COMPLETED")
                        .submittedBy(100L)
                        .downloadUrl("http://test.url")
                        .createdAt(Instant.now())
                        .build();

        when(valueOperations.get("eshop:export-job:job-123")).thenReturn(status);

        ExportStatusResponse response = exportJobService.getStatus("job-123", 100L);

        assertNotNull(response);
        assertEquals("job-123", response.getJobId());
        assertEquals("COMPLETED", response.getStatus());
        assertEquals("http://test.url", response.getDownloadUrl());
    }
}
