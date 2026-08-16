package com.eshop.app.seller.api.response;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Meta-annotation for monetary fields to enforce consistent Jackson serialization formatting.
 *
 * <h3>Expected Field Type</h3>
 *
 * <p>This annotation is intended exclusively for {@link java.math.BigDecimal} monetary fields.
 * Applying it to other types is unsupported and may result in undefined serialization behavior.
 *
 * <h3>Usage Guidance & Scope</h3>
 *
 * <p>This annotation should be used only for absolute currency amounts. It is explicitly not intended for:
 * <ul>
 *   <li>Quantities or item counts</li>
 *   <li>Percentage values (e.g. discount rates or conversion rates)</li>
 *   <li>Exchange rates</li>
 *   <li>Tax percentage rates</li>
 * </ul>
 *
 * <h3>Serialization Contract</h3>
 *
 * <ul>
 *   <li><strong>Format:</strong> Values are formatted to exactly 2 decimal places using {@link
 *       java.math.RoundingMode#HALF_UP}.
 *   <li><strong>Type:</strong> Serialized as a JSON String (e.g. {@code "1234.56"}) to prevent
 *       precision loss on clients.
 *   <li><strong>Null Behavior:</strong> If the value is {@code null}, it is serialized as a JSON
 *       {@code null}.
 * </ul>
 *
 * <h3>Currency Context</h3>
 *
 * <p>The serialized amount is in the configured currency of the context (e.g., the seller's local
 * currency). No currency symbol or ISO-4217 code is appended by the serializer.
 *
 * <h3>Target Constraints & Lombok Integration</h3>
 *
 * <p>Developers should apply this annotation only to fields. The {@code METHOD} and {@code PARAMETER}
 * targets exist solely to support Lombok-generated members (getters, builder parameters, constructor parameters)
 * copied via {@code lombok.copyableAnnotations} configuration.
 *
 * @see MoneySerializer
 * @since 1.0
 */
@Documented
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@JsonSerialize(using = MoneySerializer.class)
public @interface MonetaryField {}
