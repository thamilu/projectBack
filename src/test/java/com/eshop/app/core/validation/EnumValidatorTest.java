package com.eshop.app.core.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.eshop.app.user.domain.enums.Gender;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

class EnumValidatorTest {

    private EnumValidator validator;

    @Mock private ConstraintValidatorContext context;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        validator = new EnumValidator();

        ValidEnum annotation = Mockito.mock(ValidEnum.class);
        Mockito.doReturn(Gender.class).when(annotation).enumClass();

        validator.initialize(annotation);
    }

    @Test
    void isValid_withNullOrEmpty_shouldReturnTrue() {
        assertThat(validator.isValid(null, context)).isTrue();
        assertThat(validator.isValid("   ", context)).isTrue();
    }

    @Test
    void isValid_withExactEnumName_shouldReturnTrue() {
        assertThat(validator.isValid("MALE", context)).isTrue();
        assertThat(validator.isValid("female", context)).isTrue();
    }

    @Test
    void isValid_withDisplayName_shouldReturnTrue() {
        // "Prefer Not to Say" is the display name of PREFER_NOT_TO_SAY
        assertThat(validator.isValid("Prefer Not to Say", context)).isTrue();
        assertThat(validator.isValid("prefer not to say", context)).isTrue();
    }

    @Test
    void isValid_withCode_shouldReturnTrue() {
        // "NB" is the code for NON_BINARY
        assertThat(validator.isValid("NB", context)).isTrue();
        assertThat(validator.isValid("nb", context)).isTrue();
    }

    @Test
    void isValid_withInvalidValue_shouldReturnFalse() {
        assertThat(validator.isValid("INVALID_GENDER", context)).isFalse();
    }
}
