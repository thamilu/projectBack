package com.eshop.app.seller.api.request.validation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GstinConditionalValidatorTest {

    private GstinConditionalValidator validator;
    private ConstraintValidatorContext context;
    private ConstraintValidatorContext.ConstraintViolationBuilder builder;
    private ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext
            nodeBuilder;

    @BeforeEach
    void setUp() {
        validator = new GstinConditionalValidator();
        context = mock(ConstraintValidatorContext.class);
        builder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
        nodeBuilder =
                mock(
                        ConstraintValidatorContext.ConstraintViolationBuilder
                                .NodeBuilderCustomizableContext.class);

        when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(builder);
        when(builder.addPropertyNode(anyString())).thenReturn(nodeBuilder);
        when(nodeBuilder.addConstraintViolation()).thenReturn(context);
    }

    @Test
    void nullRootObject_shouldBeValid() {
        assertTrue(validator.isValid(null, context));
    }

    @Test
    void gstRegistered_true_gstinBlank_shouldBeInvalid() {
        GstinValidatable value = buildValue(true, null);
        assertFalse(validator.isValid(value, context));
        verify(context)
                .buildConstraintViolationWithTemplate("{validation.gstin.requiredWhenRegistered}");
    }

    @Test
    void gstRegistered_true_gstinEmpty_shouldBeInvalid() {
        GstinValidatable value = buildValue(true, "   ");
        assertFalse(validator.isValid(value, context));
        verify(context)
                .buildConstraintViolationWithTemplate("{validation.gstin.requiredWhenRegistered}");
    }

    @Test
    void gstRegistered_true_gstinPresent_shouldBeValid() {
        GstinValidatable value = buildValue(true, "29ABCDE1234F1Z5");
        assertTrue(validator.isValid(value, context));
    }

    @Test
    void gstRegistered_false_gstinBlank_shouldBeValid() {
        GstinValidatable value = buildValue(false, null);
        assertTrue(validator.isValid(value, context));
    }

    @Test
    void gstRegistered_false_gstinEmpty_shouldBeValid() {
        GstinValidatable value = buildValue(false, "");
        assertTrue(validator.isValid(value, context));
    }

    @Test
    void gstRegistered_false_gstinPresent_shouldBeInvalid() {
        GstinValidatable value = buildValue(false, "29ABCDE1234F1Z5");
        assertFalse(validator.isValid(value, context));
        verify(context)
                .buildConstraintViolationWithTemplate(
                        "{validation.gstin.notAllowedWhenNotRegistered}");
    }

    @Test
    void gstRegistered_null_gstinBlank_shouldBeValid() {
        GstinValidatable value = buildValue(null, null);
        assertTrue(validator.isValid(value, context));
    }

    @Test
    void gstRegistered_null_gstinPresent_shouldBeInvalid() {
        GstinValidatable value = buildValue(null, "29ABCDE1234F1Z5");
        assertFalse(validator.isValid(value, context));
        verify(context)
                .buildConstraintViolationWithTemplate(
                        "{validation.gstin.notAllowedWhenNotRegistered}");
    }

    private GstinValidatable buildValue(Boolean gstRegistered, String gstin) {
        return new GstinValidatable() {
            @Override
            public Boolean getGstRegistered() {
                return gstRegistered;
            }

            @Override
            public String getGstin() {
                return gstin;
            }
        };
    }
}
