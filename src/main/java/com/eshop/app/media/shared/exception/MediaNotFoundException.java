package com.eshop.app.media.shared.exception;

import com.eshop.app.core.exception.business.ResourceNotFoundException;

/**
 * Exception for media asset not found scenarios.
 *
 * <p>Thrown when a requested media asset does not exist or has been soft-deleted.
 */
public class MediaNotFoundException extends ResourceNotFoundException {

    public MediaNotFoundException(String id) {
        super("MediaAsset", "id", id);
    }

    public MediaNotFoundException(String field, String value) {
        super("MediaAsset", field, value);
    }
}
