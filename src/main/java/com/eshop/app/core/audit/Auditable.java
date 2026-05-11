package com.eshop.app.core.audit;

import com.eshop.app.enums.AuditAction;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method for automatic audit trail generation.
 *
 * <p>When placed on a service method, {@link AuditLoggingAspect} will intercept
 * the invocation and asynchronously persist an {@code AuditLog} record containing:
 * the actor (user ID, email, IP), the action, entity context, success/failure status,
 * and optionally the serialized arguments and result.
 *
 * <p>MANDATORY on all administrative, financial, and seller-modifying operations
 * per Enterprise Security & Compliance Standards.
 *
 * <pre>
 * Example usage:
 * {@code
 * @Auditable(action = AuditAction.SELLER_APPROVE, entityType = "SellerProfile")
 * public void approveSeller(Long sellerId) { ... }
 * }
 * </pre>
 *
 * @see AuditLoggingAspect
 * @see AuditAction
 * @since 2.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {

    /** The action being performed (required). */
    AuditAction action();

    /** The entity type being acted upon (e.g., "SellerProfile"). Defaults to class name. */
    String entityType() default "";

    /** Optional human-readable description of the operation. Defaults to method name. */
    String description() default "";

    /** If true, the method arguments will be serialized and stored in the audit record. */
    boolean logArgs() default false;

    /** If true, the method return value will be serialized and stored in the audit record. */
    boolean logResult() default false;
}
