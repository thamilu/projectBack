package com.eshop.app.validation;

import com.eshop.app.dto.request.ProductCreateRequest;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintValidatorContext;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class PriceDiscountValidatorUnitTest {

    private final PriceDiscountValidator validator = new PriceDiscountValidator();

    @Test
    void returnsTrue_whenNoDiscountPresent() {
        ProductCreateRequest req = ProductCreateRequest.builder()
                .price(new BigDecimal("100.00"))
                .build();
        assertTrue(validator.isValid(req, null));
    }

    @Test
    void returnsFalse_whenDiscountWithoutPrice() {
        ProductCreateRequest req = ProductCreateRequest.builder()
                .discountPrice(new BigDecimal("10.00"))
                .build();
        // Provide a mocked ConstraintValidatorContext to support chained calls
        ConstraintValidatorContext ctx = mock(ConstraintValidatorContext.class);
        ConstraintValidatorContext.ConstraintViolationBuilder builder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
        ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext node = mock(ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext.class);
        doNothing().when(ctx).disableDefaultConstraintViolation();
        when(ctx.buildConstraintViolationWithTemplate(anyString())).thenReturn(builder);
        when(builder.addPropertyNode(anyString())).thenReturn(node);
        when(node.addConstraintViolation()).thenReturn(ctx);

        assertFalse(validator.isValid(req, ctx));
    }

    @Test
    void returnsFalse_whenDiscountGreaterOrEqualPrice() {
        ProductCreateRequest req = ProductCreateRequest.builder()
                .price(new BigDecimal("100.00"))
                .discountPrice(new BigDecimal("150.00"))
                .build();
        ConstraintValidatorContext ctx = mock(ConstraintValidatorContext.class);
        ConstraintValidatorContext.ConstraintViolationBuilder builder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
        ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext node = mock(ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext.class);
        doNothing().when(ctx).disableDefaultConstraintViolation();
        when(ctx.buildConstraintViolationWithTemplate(anyString())).thenReturn(builder);
        when(builder.addPropertyNode(anyString())).thenReturn(node);
        when(node.addConstraintViolation()).thenReturn(ctx);

        assertFalse(validator.isValid(req, ctx));
    }

    @Test
    void returnsTrue_whenDiscountLessThanPrice() {
        ProductCreateRequest req = ProductCreateRequest.builder()
                .price(new BigDecimal("100.00"))
                .discountPrice(new BigDecimal("50.00"))
                .build();
        assertTrue(validator.isValid(req, null));
    }
}
