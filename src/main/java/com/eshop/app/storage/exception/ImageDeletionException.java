package com.eshop.app.storage.exception;

import com.eshop.app.core.exception.base.InfrastructureException;

public class ImageDeletionException extends InfrastructureException {
    
    public ImageDeletionException(String message) {
        super(message, "IMAGE_DELETION_FAILED");
    }

    public ImageDeletionException(String message, Throwable cause) {
        super(message, "IMAGE_DELETION_FAILED", cause);
    }
}
