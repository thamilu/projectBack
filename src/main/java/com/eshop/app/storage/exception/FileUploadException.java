package com.eshop.app.storage.exception;

import com.eshop.app.core.exception.base.InfrastructureException;

public class FileUploadException extends InfrastructureException {
    
    public FileUploadException(String message) {
        super(message, "FILE_UPLOAD_FAILED");
    }
    
    public FileUploadException(String message, Throwable cause) {
        super(message, "FILE_UPLOAD_FAILED", cause);
    }
}
