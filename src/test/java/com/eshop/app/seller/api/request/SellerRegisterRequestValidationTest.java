package com.eshop.app.seller.api.request;

import static org.junit.jupiter.api.Assertions.*;

import com.eshop.app.seller.api.request.validation.ValidationGroups;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class SellerRegisterRequestValidationTest {

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
    void testStep1Basic_shopNameSize_validation() {
        SellerRegisterRequest request = new SellerRegisterRequest();
        request.setShopName("A"); // too short, min is 2
        request.setPhone("1234567890");
        request.setBusinessPhone("1234567890");
        request.setAcceptedTerms(true);

        Set<ConstraintViolation<SellerRegisterRequest>> violations =
                validator.validate(request, ValidationGroups.Step1Basic.class);

        assertFalse(violations.isEmpty());
        assertTrue(
                violations.stream()
                        .anyMatch(v -> v.getPropertyPath().toString().equals("shopName")));
    }

    @Test
    void testStep1Basic_acceptedTerms_validation() {
        SellerRegisterRequest request = new SellerRegisterRequest();
        request.setShopName("Valid Shop");
        request.setPhone("1234567890");
        request.setBusinessPhone("1234567890");
        request.setAcceptedTerms(false); // must be true

        Set<ConstraintViolation<SellerRegisterRequest>> violations =
                validator.validate(request, ValidationGroups.Step1Basic.class);

        assertFalse(violations.isEmpty());
        assertTrue(
                violations.stream()
                        .anyMatch(v -> v.getPropertyPath().toString().equals("acceptedTerms")));
    }

    @Test
    void testStep2Kyc_panNumberSize_validation() {
        SellerRegisterRequest request = new SellerRegisterRequest();
        request.setPanNumber("ABCDE1234"); // 9 chars instead of 10

        Set<ConstraintViolation<SellerRegisterRequest>> violations =
                validator.validate(request, ValidationGroups.Step2Kyc.class);

        assertFalse(violations.isEmpty());
        assertTrue(
                violations.stream()
                        .anyMatch(v -> v.getPropertyPath().toString().equals("panNumber")));
    }

    @Test
    void testStep2Kyc_gstinBlankRejected_validation() {
        SellerRegisterRequest request = new SellerRegisterRequest();
        request.setGstin(""); // blank string must fail pattern validation

        Set<ConstraintViolation<SellerRegisterRequest>> violations =
                validator.validate(request, ValidationGroups.Step2Kyc.class);

        assertFalse(violations.isEmpty());
        assertTrue(
                violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("gstin")));
    }

    @Test
    void testStep2Kyc_aadharRedundantQuantifierRemoved_validation() {
        SellerRegisterRequest request = new SellerRegisterRequest();
        request.setAadhar("123456789012"); // starts with 1, which is invalid (must be 2-9)

        Set<ConstraintViolation<SellerRegisterRequest>> violations =
                validator.validate(request, ValidationGroups.Step2Kyc.class);

        assertFalse(violations.isEmpty());
        assertTrue(
                violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("aadhar")));
    }

    @Test
    void testStep3Bank_accountNumberSizePattern_validation() {
        SellerRegisterRequest request = new SellerRegisterRequest();
        request.setAccountNumber("123A"); // invalid format (digits only) and size (min 9)

        Set<ConstraintViolation<SellerRegisterRequest>> violations =
                validator.validate(request, ValidationGroups.Step3Bank.class);

        assertFalse(violations.isEmpty());
        assertTrue(
                violations.stream()
                        .anyMatch(v -> v.getPropertyPath().toString().equals("accountNumber")));
    }

    @Test
    void testStep5Address_addressRequestValid() {
        AddressRequest address =
                new AddressRequest(
                        "123 MG Road",
                        "Suite 4B",
                        "Bengaluru",
                        "Bengaluru Urban",
                        "North",
                        "Karnataka",
                        "560001",
                        "India",
                        "https://maps.app.goo.gl/xyz");
        Set<ConstraintViolation<AddressRequest>> violations =
                validator.validate(address, ValidationGroups.Step5Address.class);
        assertTrue(violations.isEmpty(), "Valid address request should pass validation");
    }

    @Test
    void testStep5Address_addressRequestBlank_UsesPropertiesKey() {
        AddressRequest address = new AddressRequest();
        Set<ConstraintViolation<AddressRequest>> violations =
                validator.validate(address, ValidationGroups.Step5Address.class);

        assertFalse(violations.isEmpty());
        assertTrue(
                violations.stream()
                        .anyMatch(
                                v ->
                                        v.getPropertyPath().toString().equals("addressLine1")
                                                && v.getMessage()
                                                        .equals(
                                                                "Primary address line is"
                                                                        + " required")),
                "Error message should resolve from properties file");
        assertTrue(
                violations.stream()
                        .anyMatch(
                                v ->
                                        v.getPropertyPath().toString().equals("city")
                                                && v.getMessage().equals("City is required")),
                "Error message should resolve from properties file");
    }

    @Test
    void testStep5Address_addressLineSpamPattern_validation() {
        AddressRequest address =
                new AddressRequest(
                        "@@@@@@@@@@@@",
                        null,
                        "Bengaluru",
                        null,
                        null,
                        "Karnataka",
                        "560001",
                        "India",
                        null);
        Set<ConstraintViolation<AddressRequest>> violations =
                validator.validate(address, ValidationGroups.Step5Address.class);

        assertFalse(violations.isEmpty(), "Special characters spam should fail pattern validation");
        assertTrue(
                violations.stream()
                        .anyMatch(
                                v ->
                                        v.getPropertyPath().toString().equals("addressLine1")
                                                && v.getMessage()
                                                        .equals(
                                                                "Primary address line contains"
                                                                        + " invalid characters")),
                "Error message should resolve from properties file");
    }

    @Test
    void testStep5Address_geographicFieldsMinLength_validation() {
        AddressRequest address =
                new AddressRequest(
                        "123 MG Road",
                        null,
                        "B",
                        null,
                        null,
                        "Karnataka",
                        "560001",
                        "India",
                        null // city too short, min is 2
                        );
        Set<ConstraintViolation<AddressRequest>> violations =
                validator.validate(address, ValidationGroups.Step5Address.class);

        assertFalse(
                violations.isEmpty(), "Geographic fields with length < 2 should fail validation");
        assertTrue(
                violations.stream()
                        .anyMatch(
                                v ->
                                        v.getPropertyPath().toString().equals("city")
                                                && v.getMessage()
                                                        .equals(
                                                                "City name must be between 2 and"
                                                                        + " 100 characters")),
                "Error message should resolve from properties file");
    }

    @Test
    void testStep5Address_googleMapsUrl_validation() {
        // Invalid URL
        AddressRequest address =
                new AddressRequest(
                        "123 MG Road",
                        null,
                        "Bengaluru",
                        null,
                        null,
                        "Karnataka",
                        "560001",
                        "India",
                        "https://invalid-url.com");
        Set<ConstraintViolation<AddressRequest>> violations =
                validator.validate(address, ValidationGroups.Step5Address.class);

        assertFalse(violations.isEmpty(), "Invalid Google Maps URL should fail");
        assertTrue(
                violations.stream()
                        .anyMatch(
                                v ->
                                        v.getPropertyPath().toString().equals("googleMapsUrl")
                                                && v.getMessage()
                                                        .equals(
                                                                "Google Maps URL must be a valid"
                                                                        + " google.com/maps or"
                                                                        + " maps.app.goo.gl HTTPS"
                                                                        + " link")),
                "Error message should resolve from properties file");
    }
}
