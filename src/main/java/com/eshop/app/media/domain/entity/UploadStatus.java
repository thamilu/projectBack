package com.eshop.app.media.domain.entity;

/**
 * Upload lifecycle status for media assets.
 *
 * <p>Tracks the state of an upload from initial presigned URL generation through confirmation or
 * cleanup.
 */
public enum UploadStatus {

    /** Presigned URL generated, awaiting frontend upload + confirmation. */
    PENDING,

    /** Upload confirmed by frontend — asset is active and linked. */
    CONFIRMED,

    /** Upload failed during frontend upload or confirmation. */
    FAILED,

    /** Identified as orphan by cleanup job (PENDING too long). */
    ORPHANED,

    /** Soft-deleted by user action. */
    DELETED
}
