package com.eshop.app.media.shared.exception;

import com.eshop.app.core.exception.business.ValidationException;

/**
 * Exception for media upload validation failures.
 *
 * <p>Thrown when upload requests fail validation (invalid MIME type, file too large, max images
 * exceeded, etc.).
 */
public class MediaUploadException extends ValidationException {

    public MediaUploadException(String message) {
        super(message);
    }
}
