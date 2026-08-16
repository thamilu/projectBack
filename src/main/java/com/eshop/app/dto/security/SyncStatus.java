package com.eshop.app.dto.security;

/** Status of Keycloak identity and role syncing with the local database. */
public enum SyncStatus {
    /** Sync is in progress. */
    PENDING,

    /** Sync completed successfully. */
    COMPLETED,

    /** Sync failed after retries. */
    FAILED
}
