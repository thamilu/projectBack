package com.eshop.app.core.exception.business;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidationExceptionTest {

    @Test
    @DisplayName("Should initialize with default error code and BAD_REQUEST status")
    void testSingleArgConstructor() {
        ValidationException ex = new ValidationException("Validation failed");

        assertThat(ex.getMessage()).isEqualTo("Validation failed");
        assertThat(ex.getErrorCode()).isEqualTo("VALIDATION_ERROR");
        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getFieldErrors()).isEmpty();
    }

    @Test
    @DisplayName("Should initialize with custom error code")
    void testTwoArgConstructor() {
        ValidationException ex = new ValidationException("Validation failed", "CUSTOM_CODE");

        assertThat(ex.getMessage()).isEqualTo("Validation failed");
        assertThat(ex.getErrorCode()).isEqualTo("CUSTOM_CODE");
        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Should initialize with custom error code and status")
    void testThreeArgConstructor() {
        ValidationException ex = new ValidationException("Validation failed", "CUSTOM_CODE", HttpStatus.UNPROCESSABLE_CONTENT);

        assertThat(ex.getMessage()).isEqualTo("Validation failed");
        assertThat(ex.getErrorCode()).isEqualTo("CUSTOM_CODE");
        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
    }

    @Test
    @DisplayName("Should add field error and return unmodifiable list from getter")
    void testAddFieldErrorAndUnmodifiableList() {
        ValidationException ex = new ValidationException("Validation error");
        ex.addFieldError("email", "must not be blank");
        ex.addFieldError("age", "must be positive", -5);

        assertThat(ex.getFieldErrors()).hasSize(2);
        assertThat(ex.getFieldErrors().get(0).getField()).isEqualTo("email");
        assertThat(ex.getFieldErrors().get(0).getMessage()).isEqualTo("must not be blank");
        assertThat(ex.getFieldErrors().get(0).getRejectedValue()).isNull();

        assertThat(ex.getFieldErrors().get(1).getField()).isEqualTo("age");
        assertThat(ex.getFieldErrors().get(1).getMessage()).isEqualTo("must be positive");
        assertThat(ex.getFieldErrors().get(1).getRejectedValue()).isEqualTo(-5);

        assertThatThrownBy(() -> ex.getFieldErrors().add(new ValidationException.FieldError("test", "msg")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Should throw NullPointerException when field or message is null")
    void testNullValidationInAddFieldError() {
        ValidationException ex = new ValidationException("Validation error");

        assertThatThrownBy(() -> ex.addFieldError(null, "message"))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("field must not be null");

        assertThatThrownBy(() -> ex.addFieldError("field", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("message must not be null");
    }

    @Test
    @DisplayName("Should generate diagnostic string including field errors")
    void testToStringOverride() {
        ValidationException ex = new ValidationException("Invalid input");
        assertThat(ex.toString()).doesNotContain("fieldErrors=");

        ex.addFieldError("email", "invalid format");
        ex.addFieldError("password", "too short", "secret123");

        String toString = ex.toString();
        assertThat(toString).contains("fieldErrors=email: invalid format; password: too short");
        assertThat(toString).doesNotContain("secret123"); // Security check: rejected values excluded from toString
    }

    @Test
    @DisplayName("Should serialize and deserialize correctly without throwing NotSerializableException")
    void testSerialization() throws Exception {
        ValidationException ex = new ValidationException("Serialization test");
        ex.addFieldError("username", "already taken", "john_doe");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(ex);
        }

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ValidationException deserializedEx;
        try (ObjectInputStream ois = new ObjectInputStream(bais)) {
            deserializedEx = (ValidationException) ois.readObject();
        }

        assertThat(deserializedEx.getMessage()).isEqualTo("Serialization test");
        assertThat(deserializedEx.getFieldErrors()).hasSize(1);
        assertThat(deserializedEx.getFieldErrors().get(0).getField()).isEqualTo("username");
        assertThat(deserializedEx.getFieldErrors().get(0).getMessage()).isEqualTo("already taken");
        // rejectedValue is transient, so expected to be null after deserialization
        assertThat(deserializedEx.getFieldErrors().get(0).getRejectedValue()).isNull();
    }
}
