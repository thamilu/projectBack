package com.eshop.app.storage.exception;

import com.eshop.app.core.exception.base.InfrastructureException;

public class ImageUploadException extends InfrastructureException {
    
    public ImageUploadException(String message) {
        super(message, "IMAGE_UPLOAD_FAILED");
    }

    public ImageUploadException(String message, Throwable cause) {
        super(message, "IMAGE_UPLOAD_FAILED", cause);
    }
}
