package com.eshop.app.user.application.security;

import com.eshop.app.core.api.response.BulkOperationResult;
import java.lang.annotation.*;

/** Annotation to enforce API idempotency checks on POST endpoints. */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface IdempotentOperation {

    /** Operation type key used for idempotency namespace separation in Redis. */
    String operationType();

    /** Result type to deserialize cached response into. */
    Class<?> resultType() default BulkOperationResult.class;
}
