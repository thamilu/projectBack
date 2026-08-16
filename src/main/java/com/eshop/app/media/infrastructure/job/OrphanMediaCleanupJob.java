package com.eshop.app.media.infrastructure.job;

import com.eshop.app.media.domain.entity.MediaAsset;
import com.eshop.app.media.domain.entity.UploadStatus;
import com.eshop.app.media.domain.repository.MediaAssetRepository;
import com.eshop.app.media.shared.constants.MediaConstants;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

/**
 * Scheduled job for cleaning up orphaned media assets.
 *
 * <p>Orphans occur when:
 *
 * <ul>
 *   <li>A presigned URL is generated but the upload never completes
 *   <li>The upload completes but confirmation never arrives
 *   <li>Product creation fails after image upload
 * </ul>
 *
 * <p>Runs every hour, protected by ShedLock for distributed safety. Finds PENDING assets older than
 * 24 hours and marks them as ORPHANED.
 *
 * @author E-Shop Team
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrphanMediaCleanupJob {

    private final MediaAssetRepository mediaAssetRepository;
    private final ObjectProvider<S3AsyncClient> s3AsyncClientProvider;
    private final com.eshop.app.core.infrastructure.config.properties.AppProperties appProperties;

    /**
     * Clean up orphaned media assets.
     *
     * <p>Schedule: Every hour. Lock: Held for at most 30 minutes, minimum 5 minutes.
     */
    @Scheduled(cron = "0 0 * * * *")
    @SchedulerLock(name = "OrphanMediaCleanupJob", lockAtMostFor = "PT30M", lockAtLeastFor = "PT5M")
    @Transactional
    public void cleanupOrphanedAssets() {
        LocalDateTime cutoff = LocalDateTime.now().minus(MediaConstants.ORPHAN_CLEANUP_THRESHOLD);

        log.info("Starting orphan media cleanup. cutoff={}", cutoff);

        List<MediaAsset> orphans =
                mediaAssetRepository.findByUploadStatusAndCreatedAtBefore(
                        UploadStatus.PENDING, cutoff);

        if (orphans.isEmpty()) {
            log.info("No orphaned media assets found");
            return;
        }

        int cleaned = 0;
        int failed = 0;
        String bucketName = appProperties.getStorage().getR2().getBucketName();

        for (MediaAsset orphan : orphans) {
            try {
                // Attempt to delete from R2 storage (best-effort)
                deleteFromR2(orphan.getStorageKey(), bucketName);

                // Mark as orphaned in database
                orphan.markOrphaned();
                mediaAssetRepository.save(orphan);
                cleaned++;

                log.debug(
                        "Cleaned orphan. assetId={}, storageKey={}, age={}",
                        orphan.getId(),
                        orphan.getStorageKey(),
                        java.time.Duration.between(orphan.getCreatedAt(), LocalDateTime.now()));

            } catch (Exception e) {
                failed++;
                log.error(
                        "Failed to clean orphan. assetId={}, storageKey={}, error={}",
                        orphan.getId(),
                        orphan.getStorageKey(),
                        e.getMessage());
            }
        }

        log.info(
                "Orphan media cleanup completed. total={}, cleaned={}, failed={}",
                orphans.size(),
                cleaned,
                failed);
    }

    /**
     * Best-effort deletion from R2 storage. Failures are logged but do not prevent the orphan from
     * being marked.
     */
    private void deleteFromR2(String storageKey, String bucketName) {
        S3AsyncClient s3AsyncClient = s3AsyncClientProvider.getIfAvailable();
        if (s3AsyncClient == null) {
            log.warn("R2 delete skipped: S3AsyncClient is not configured. Key: {}", storageKey);
            return;
        }
        try {
            s3AsyncClient
                    .deleteObject(
                            DeleteObjectRequest.builder()
                                    .bucket(bucketName)
                                    .key(storageKey)
                                    .build())
                    .join();
        } catch (Exception e) {
            log.warn(
                    "R2 delete failed for orphan cleanup. key={}, error={}",
                    storageKey,
                    e.getMessage());
        }
    }
}
