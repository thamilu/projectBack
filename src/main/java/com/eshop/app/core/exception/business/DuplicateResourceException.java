package com.eshop.app.core.exception.business;

import com.eshop.app.core.exception.base.BusinessException;
import org.springframework.http.HttpStatus;

public class DuplicateResourceException extends BusinessException {
    private final String resourceName;
    private final String fieldName;
    private final String fieldValue;

    public DuplicateResourceException(String resourceName, String fieldName, String fieldValue) {
        super(String.format("%s with %s '%s' already exists", resourceName, fieldName, fieldValue), "DUPLICATE_RESOURCE", HttpStatus.CONFLICT);
        this.resourceName = resourceName;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }

    public DuplicateResourceException(String message) {
        super(message, "DUPLICATE_RESOURCE", HttpStatus.CONFLICT);
        this.resourceName = null;
        this.fieldName = null;
        this.fieldValue = null;
    }

    public DuplicateResourceException(String message, Throwable cause) {
        super(message, "DUPLICATE_RESOURCE", cause);
        this.resourceName = null;
        this.fieldName = null;
        this.fieldValue = null;
    }

    public String getResourceName() { return resourceName; }
    public String getFieldName() { return fieldName; }
    public String getFieldValue() { return fieldValue; }
}
