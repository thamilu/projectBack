package com.eshop.app.validation;

import com.eshop.app.dto.request.ProductCreateRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class ProductCreateRequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    public static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    public static void tearDown() {
        factory.close();
    }

    @Test
    void discountGreaterThanPrice_isInvalid() {
        ProductCreateRequest req = ProductCreateRequest.builder()
                .name("Test Product")
                .sku("TP-001")
                .price(new BigDecimal("100.00"))
                .discountPrice(new BigDecimal("150.00"))
                .categoryId(1L)
                .storeId(1L)
                .build();

        Set<ConstraintViolation<ProductCreateRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty(), "Expected violations for discount >= price");
        Set<String> messages = violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.toSet());
        assertTrue(messages.stream().anyMatch(m -> m.toLowerCase().contains("discount")),
                "Expected discount-related message");
    }

    @Test
    void discountWithoutPrice_isInvalid() {
        ProductCreateRequest req = ProductCreateRequest.builder()
                .name("Test Product")
                .sku("TP-002")
                .discountPrice(new BigDecimal("10.00"))
                .categoryId(1L)
                .storeId(1L)
                .build();

        Set<ConstraintViolation<ProductCreateRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty(), "Expected violations when discountPrice present but price missing");
        Set<String> messages = violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.toSet());
        assertTrue(messages.stream().anyMatch(
                m -> m.toLowerCase().contains("regular price") || m.toLowerCase().contains("price is required")),
                "Expected price-related message");
    }

    @Test
    void tooManyTags_isInvalid() {
        Set<String> tags = new HashSet<>();
        for (int i = 0; i < 25; i++) {
            tags.add("tag" + i);
        }

        ProductCreateRequest req = ProductCreateRequest.builder()
                .name("Test Product")
                .sku("TP-003")
                .price(new BigDecimal("50.00"))
                .categoryId(1L)
                .storeId(1L)
                .tags(tags)
                .build();

        Set<ConstraintViolation<ProductCreateRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty(), "Expected violations when too many tags");
        boolean found = violations.stream().anyMatch(v -> v.getPropertyPath().toString().contains("tags"));
        assertTrue(found, "Expected a tags-related violation");
    }
}
