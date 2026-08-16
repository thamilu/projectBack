package com.eshop.app.seller.api.request.validation;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.validation.GroupSequence;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class ValidationGroupsTest {

    @Test
    void fullRegistrationSequence_groupSequenceAnnotation_shouldContainAllStepsInOrder() {
        GroupSequence sequence =
                ValidationGroups.FullRegistrationSequence.class.getAnnotation(GroupSequence.class);

        assertNotNull(sequence, "@GroupSequence annotation must be present");
        List<Class<?>> groups = Arrays.asList(sequence.value());

        assertEquals(5, groups.size(), "Should contain exactly 5 steps");
        assertEquals(ValidationGroups.Step1Basic.class, groups.get(0));
        assertEquals(ValidationGroups.Step2Kyc.class, groups.get(1));
        assertEquals(ValidationGroups.Step3Bank.class, groups.get(2));
        assertEquals(ValidationGroups.Step4Optional.class, groups.get(3));
        assertEquals(ValidationGroups.Step5Address.class, groups.get(4));
    }

    @Test
    void fullRegistrationAggregate_shouldExtendAllStepGroups() {
        Class<?>[] interfaces = ValidationGroups.FullRegistrationAggregate.class.getInterfaces();
        List<Class<?>> extendedList = Arrays.asList(interfaces);

        assertEquals(5, extendedList.size(), "Should extend exactly 5 steps");
        assertTrue(extendedList.contains(ValidationGroups.Step1Basic.class));
        assertTrue(extendedList.contains(ValidationGroups.Step2Kyc.class));
        assertTrue(extendedList.contains(ValidationGroups.Step3Bank.class));
        assertTrue(extendedList.contains(ValidationGroups.Step4Optional.class));
        assertTrue(extendedList.contains(ValidationGroups.Step5Address.class));
    }

    @Test
    void validationGroups_privateConstructor_shouldThrowOnReflectionInstantiation()
            throws Exception {
        Constructor<ValidationGroups> constructor = ValidationGroups.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        InvocationTargetException exception =
                assertThrows(
                        InvocationTargetException.class,
                        constructor::newInstance,
                        "Instantiation via reflection should throw an exception");

        assertTrue(exception.getCause() instanceof UnsupportedOperationException);
        assertEquals(
                "ValidationGroups is a utility class and cannot be instantiated",
                exception.getCause().getMessage());
    }
}
