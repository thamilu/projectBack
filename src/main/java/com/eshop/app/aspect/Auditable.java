package com.eshop.app.aspect;

import com.eshop.app.enums.AuditAction;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark methods for audit logging.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {

    /** The action being performed. */
    AuditAction action();

    /** The type of entity being audited (e.g., "Product"). */
    String entityType() default "";

    /** Optional description of the operation. */
    String description() default "";

    /** Whether to serialize and store the method arguments. */
    boolean logArgs() default false;

    /** Whether to serialize and store the method result. */
    boolean logResult() default false;
}
