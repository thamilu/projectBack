package com.eshop.app.core.api.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class BatchDeleteRequestValidationTest {

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
    void validRequest_hasNoViolations() {
        BatchDeleteRequest request = new BatchDeleteRequest(Set.of(1L, 2L, 3L), null);
        Set<ConstraintViolation<BatchDeleteRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Valid ID set should have no validation errors");
    }

    @Test
    void emptySet_isRejected() {
        BatchDeleteRequest request = new BatchDeleteRequest(Set.of(), null);
        Set<ConstraintViolation<BatchDeleteRequest>> violations = validator.validate(request);
        assertTrue(
                violations.stream().anyMatch(v -> v.getMessage().equals("ID set cannot be empty")));
    }

    @Test
    void nullElement_isRejectedRatherThanReachingTheServiceLayer() {
        Set<Long> idsWithNull = new HashSet<>();
        idsWithNull.add(1L);
        idsWithNull.add(null);
        BatchDeleteRequest request = new BatchDeleteRequest(idsWithNull, null);

        Set<ConstraintViolation<BatchDeleteRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty(), "A null element in the ID set must fail validation");
    }

    @Test
    void nonPositiveElement_isRejected() {
        BatchDeleteRequest request = new BatchDeleteRequest(Set.of(1L, -5L), null);
        Set<ConstraintViolation<BatchDeleteRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "A non-positive ID must fail validation");
    }

    @Test
    void overMaxBatchSize_isRejectedWithInterpolatedMessage() {
        Set<Long> tooMany =
                LongStream.rangeClosed(1, 101).boxed().collect(Collectors.toCollection(LinkedHashSet::new));
        BatchDeleteRequest request = new BatchDeleteRequest(tooMany, null);

        Set<ConstraintViolation<BatchDeleteRequest>> violations = validator.validate(request);

        assertTrue(
                violations.stream()
                        .anyMatch(v -> v.getMessage().equals("Maximum 100 items per batch delete")),
                "{max} should interpolate to the actual configured limit");
    }

    @Test
    void ids_isDefensivelyCopiedAndUnmodifiable() {
        Set<Long> mutableSource = new HashSet<>(Set.of(1L, 2L));
        BatchDeleteRequest request = new BatchDeleteRequest(mutableSource, null);

        mutableSource.add(3L);

        assertEquals(2, request.ids().size(), "Post-construction mutation of the caller's set must not leak in");
        assertThrows(UnsupportedOperationException.class, () -> request.ids().add(99L));
    }

    @Test
    void nullIds_preservedAsNullForNotEmptyToReport() {
        BatchDeleteRequest request = new BatchDeleteRequest(null, null);
        Set<ConstraintViolation<BatchDeleteRequest>> violations = validator.validate(request);
        assertTrue(
                violations.stream().anyMatch(v -> v.getMessage().equals("ID set cannot be empty")),
                "Null ids should surface as the @NotEmpty violation, not an NPE during construction");
    }

    @Test
    void nullOptions_defaultsToNonAtomicMode() {
        BatchDeleteRequest request = new BatchDeleteRequest(Set.of(1L), null);
        assertFalse(request.options().atomicMode());
    }
}
