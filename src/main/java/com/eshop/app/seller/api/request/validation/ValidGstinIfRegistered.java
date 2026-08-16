package com.eshop.app.seller.api.request.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * Class-level annotation for conditional GSTIN validation.
 *
 * <p>Enforces that the GST Identification Number (GSTIN) is provided when the seller has declared
 * that they are GST registered. Conversely, it enforces that GSTIN is not provided when they are
 * not registered.
 *
 * <h3>Usage</h3>
 *
 * <p>Apply this annotation to DTO classes that implement {@link GstinValidatable}:
 *
 * <pre>{@code
 * @ValidGstinIfRegistered(groups = ValidationGroups.Step2Kyc.class)
 * public class SellerRegisterRequest implements GstinValidatable {
 *     private Boolean gstRegistered;
 *     private String gstin;
 *
 *     @Override
 *     public Boolean getGstRegistered() { return gstRegistered; }
 *
 *     @Override
 *     public String getGstin() { return gstin; }
 * }
 * }</pre>
 *
 * <h3>Validation Rules</h3>
 *
 * <ul>
 *   <li>If {@code gstRegistered == true} and {@code gstin} is blank → invalid
 *   <li>If {@code gstRegistered != true} and {@code gstin} is non-blank → invalid (contradictory
 *       registration declaration state)
 * </ul>
 *
 * @see GstinValidatable
 * @see GstinConditionalValidator
 * @since 1.0
 */
@Documented
@Constraint(validatedBy = GstinConditionalValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidGstinIfRegistered {

    /**
     * Error message template key.
     *
     * <p>Defaults to the localized key in {@code ValidationMessages.properties}.
     *
     * @return the error message template
     */
    String message() default "{validation.gstin.requiredWhenRegistered}";

    /**
     * Validation groups to which this constraint belongs.
     *
     * @return the validation groups
     */
    Class<?>[] groups() default {};

    /**
     * Payload associated with this constraint.
     *
     * @return the payload
     */
    Class<? extends Payload>[] payload() default {};
}
