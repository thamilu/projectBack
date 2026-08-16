package com.eshop.app.seller.api.request;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class RejectionRequestValidationTest {

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
    void testReasonValid() {
        RejectionRequest request =
                new RejectionRequest(
                        "This is a valid rejection reason of more than ten characters.");
        Set<ConstraintViolation<RejectionRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Valid reason should have no validation errors");
    }

    @Test
    void testReasonWithPunctuationAndNewlines() {
        RejectionRequest request =
                new RejectionRequest(
                        "Reason: Uploaded GST card details are expired!\n"
                                + "Please re-upload a valid, non-expired copy of the card.");
        Set<ConstraintViolation<RejectionRequest>> violations = validator.validate(request);
        assertTrue(
                violations.isEmpty(),
                "Reason containing punctuation, quotes, and newlines should be valid");
    }

    @Test
    void testReasonBlank_UsesPropertiesKey() {
        RejectionRequest request = new RejectionRequest("   ");
        Set<ConstraintViolation<RejectionRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Blank reason should trigger validation errors");
        assertTrue(
                violations.stream()
                        .anyMatch(v -> v.getMessage().equals("Rejection reason is required")),
                "Error message should be resolved from ValidationMessages.properties");
    }

    @Test
    void testReasonTooShort_UsesPropertiesKey() {
        RejectionRequest request = new RejectionRequest("Short"); // 5 characters, min is 10
        Set<ConstraintViolation<RejectionRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Reason shorter than 10 characters should fail");
        assertTrue(
                violations.stream()
                        .anyMatch(
                                v ->
                                        v.getMessage()
                                                .equals(
                                                        "Rejection reason must be between 10 and"
                                                                + " 500 characters")),
                "Error message should be resolved from ValidationMessages.properties");
    }

    @Test
    void testReasonInvalidPattern_SpecialCharsSpam() {
        RejectionRequest request =
                new RejectionRequest("!!!!!!!!!!!!"); // 12 characters, but only special chars
        Set<ConstraintViolation<RejectionRequest>> violations = validator.validate(request);
        assertFalse(
                violations.isEmpty(),
                "Reason with only special characters should fail pattern validation");
        assertTrue(
                violations.stream()
                        .anyMatch(
                                v ->
                                        v.getMessage()
                                                .equals(
                                                        "Rejection reason contains invalid"
                                                            + " characters or does not contain any"
                                                            + " letters")),
                "Error message should be resolved from ValidationMessages.properties");
    }

    @Test
    void testReasonInvalidPattern_DigitsOnly() {
        RejectionRequest request =
                new RejectionRequest("123456789012"); // 12 characters, but no letters
        Set<ConstraintViolation<RejectionRequest>> violations = validator.validate(request);
        assertFalse(
                violations.isEmpty(),
                "Reason with only digits (no letters) should fail pattern validation");
        assertTrue(
                violations.stream()
                        .anyMatch(
                                v ->
                                        v.getMessage()
                                                .equals(
                                                        "Rejection reason contains invalid"
                                                            + " characters or does not contain any"
                                                            + " letters")),
                "Error message should be resolved from ValidationMessages.properties");
    }

    @Test
    void testReasonTooLong() {
        StringBuilder longReason = new StringBuilder();
        for (int i = 0; i < 51; i++) {
            longReason.append("abcdefghij"); // 51 * 10 = 510 characters, max is 500
        }
        RejectionRequest request = new RejectionRequest(longReason.toString());
        Set<ConstraintViolation<RejectionRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Reason longer than 500 characters should fail");
        assertTrue(
                violations.stream()
                        .anyMatch(
                                v ->
                                        v.getMessage()
                                                .equals(
                                                        "Rejection reason must be between 10 and"
                                                                + " 500 characters")),
                "Error message should be resolved from ValidationMessages.properties");
    }
}
