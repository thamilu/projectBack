package com.eshop.app.controller;

import com.eshop.app.config.properties.AppProperties;
import com.eshop.app.constants.ApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

@Tag(name = "Payment Methods", description = "Payment method discovery and validation")
@RestController
@RequestMapping(ApiConstants.Endpoints.PAYMENT_METHODS)
@RequiredArgsConstructor
@SecurityRequirement(name = "Keycloak OAuth2")
@SecurityRequirement(name = "Bearer Authentication")
public class PaymentMethodsController {
    
    private final AppProperties appProperties;
    
    @GetMapping("/available")
    @Operation(summary = "Get Available Payment Methods", 
               description = "Get all available payment methods based on user location and order amount")
    @ApiResponse(responseCode = "200", description = "List of available payment methods")
    @PreAuthorize("hasRole(@appProperties.security.roles.customer) or hasRole(@appProperties.security.roles.admin)")
    public ResponseEntity<Map<String, Object>> getAvailablePaymentMethods(
            @Parameter(description = "Order amount") @RequestParam(required = false) BigDecimal amount,
            @Parameter(description = "Currency code") @RequestParam(required = false) String currency,
            @Parameter(description = "Country code") @RequestParam(defaultValue = "IN") String country) {
        
        AppProperties.Business business = appProperties.getBusiness();
        String defaultCurrency = (currency != null && !currency.isBlank()) ? currency : business.getDefaultCurrency();

        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> cardMethods = new ArrayList<>();
        
        if ("IN".equals(country)) {
            cardMethods.add(createPaymentMethod("CREDIT_CARD_VISA", "Visa Credit Card", "card", true, null));
            cardMethods.add(createPaymentMethod("CREDIT_CARD_MASTERCARD", "Mastercard Credit", "card", true, null));
            cardMethods.add(createPaymentMethod("CREDIT_CARD_RUPAY", "RuPay Credit Card", "card", true, null));
            cardMethods.add(createPaymentMethod("DEBIT_CARD_VISA", "Visa Debit Card", "card", true, null));
            cardMethods.add(createPaymentMethod("DEBIT_CARD_MASTERCARD", "Mastercard Debit", "card", true, null));
            cardMethods.add(createPaymentMethod("DEBIT_CARD_RUPAY", "RuPay Debit Card", "card", true, null));
        } else {
            cardMethods.add(createPaymentMethod("CREDIT_CARD_VISA", "Visa Credit Card", "card", true, null));
            cardMethods.add(createPaymentMethod("CREDIT_CARD_MASTERCARD", "Mastercard Credit", "card", true, null));
            cardMethods.add(createPaymentMethod("CREDIT_CARD_AMERICAN_EXPRESS", "American Express", "card", true, null));
            cardMethods.add(createPaymentMethod("DEBIT_CARD_VISA", "Visa Debit Card", "card", true, null));
            cardMethods.add(createPaymentMethod("DEBIT_CARD_MASTERCARD", "Mastercard Debit", "card", true, null));
        }

        List<Map<String, Object>> upiMethods = new ArrayList<>();
        if ("IN".equals(country) && business.isUpiEnabled()) {
            upiMethods.add(createPaymentMethod("UPI", "UPI (Any App)", "upi", true, "Pay using any UPI app"));
            upiMethods.add(createPaymentMethod("UPI_GOOGLEPAY", "Google Pay", "upi", true, "googlepay://"));
            upiMethods.add(createPaymentMethod("UPI_PHONEPE", "PhonePe", "upi", true, "phonepe://"));
            upiMethods.add(createPaymentMethod("UPI_PAYTM", "Paytm UPI", "upi", true, "paytmmp://"));
        }

        List<Map<String, Object>> walletMethods = new ArrayList<>();
        if ("IN".equals(country)) {
            walletMethods.add(createPaymentMethod("PAYTM_WALLET", "Paytm Wallet", "wallet", true, null));
            walletMethods.add(createPaymentMethod("PHONEPE_WALLET", "PhonePe Wallet", "wallet", true, null));
        }
        walletMethods.add(createPaymentMethod("PAYPAL", "PayPal", "wallet", true, null));

        List<Map<String, Object>> emiMethods = new ArrayList<>();
        BigDecimal emiMin = BigDecimal.valueOf(business.getEmiMinAmount());
        if (business.isEmiEnabled() && amount != null && amount.compareTo(emiMin) >= 0) {
            emiMethods.add(createEmiMethod("EMI", "EMI (3-60 months)", "emi", true, 3, 60, emiMin));
        }

        response.put("cards", cardMethods);
        response.put("upi", upiMethods);
        response.put("wallets", walletMethods);
        response.put("emi", emiMethods);
        response.put("currency", defaultCurrency);
        response.put("country", country);
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/validate-card")
    @Operation(summary = "Validate Card Details", 
               description = "Validate credit/debit card number and detect card type")
    @ApiResponse(responseCode = "200", description = "Card validation result")
    @PreAuthorize("hasRole(@appProperties.security.roles.customer) or hasRole(@appProperties.security.roles.admin)")
    public ResponseEntity<Map<String, Object>> validateCard(@RequestBody Map<String, String> cardData) {
        String cardNumber = cardData.get("cardNumber");
        Map<String, Object> result = new HashMap<>();
        
        if (cardNumber == null || cardNumber.isBlank()) {
            result.put("valid", false);
            result.put("error", "Card number is required");
            return ResponseEntity.ok(result);
        }

        String cleanCardNumber = cardNumber.replaceAll("\\D", "");
        if (cleanCardNumber.length() < 13 || cleanCardNumber.length() > 19) {
            result.put("valid", false);
            result.put("error", "Invalid card number length");
            return ResponseEntity.ok(result);
        }
        
        if (!isValidLuhn(cleanCardNumber)) {
            result.put("valid", false);
            result.put("error", "Invalid card number");
            return ResponseEntity.ok(result);
        }

        result.put("valid", true);
        result.put("cardBrand", detectCardBrand(cleanCardNumber));
        result.put("lastFour", cleanCardNumber.substring(cleanCardNumber.length() - 4));
        
        return ResponseEntity.ok(result);
    }
    
    @PostMapping("/validate-upi")
    @Operation(summary = "Validate UPI ID", description = "Validate UPI Virtual Payment Address (VPA)")
    @ApiResponse(responseCode = "200", description = "UPI validation result")
    @PreAuthorize("hasRole(@appProperties.security.roles.customer) or hasRole(@appProperties.security.roles.admin)")
    public ResponseEntity<Map<String, Object>> validateUpi(@RequestBody Map<String, String> upiData) {
        String upiId = upiData.get("upiId");
        Map<String, Object> result = new HashMap<>();
        
        if (upiId == null || upiId.isBlank()) {
            result.put("valid", false);
            result.put("error", "UPI ID is required");
            return ResponseEntity.ok(result);
        }

        String upiPattern = "^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+$";
        if (!upiId.matches(upiPattern)) {
            result.put("valid", false);
            result.put("error", "Invalid UPI ID format");
            return ResponseEntity.ok(result);
        }

        result.put("valid", true);
        return ResponseEntity.ok(result);
    }
    
    @GetMapping("/gateways")
    @Operation(summary = "Get Payment Gateways")
    @PreAuthorize("hasRole(@appProperties.security.roles.admin)")
    public ResponseEntity<Map<String, Object>> getPaymentGateways() {
        Map<String, Object> gateways = new HashMap<>();

        Map<String, Object> stripe = new HashMap<>();
        stripe.put("name", "Stripe");
        stripe.put("supportedMethods", Arrays.asList("CREDIT_CARD", "DEBIT_CARD"));
        stripe.put("currencies", Arrays.asList("USD", "EUR", "GBP", "INR"));
        gateways.put("STRIPE", stripe);

        Map<String, Object> razorpay = new HashMap<>();
        razorpay.put("name", "Razorpay");
        razorpay.put("supportedMethods", Arrays.asList("CREDIT_CARD", "DEBIT_CARD", "UPI", "WALLETS"));
        razorpay.put("currencies", Arrays.asList("INR"));
        gateways.put("RAZORPAY", razorpay);

        return ResponseEntity.ok(gateways);
    }

    private Map<String, Object> createPaymentMethod(String code, String name, String type, boolean enabled, String description) {
        Map<String, Object> method = new HashMap<>();
        method.put("code", code);
        method.put("name", name);
        method.put("type", type);
        method.put("enabled", enabled);
        if (description != null)
            method.put("description", description);
        return method;
    }
    
    private Map<String, Object> createEmiMethod(String code, String name, String type, boolean enabled, int minTenure,
            int maxTenure, BigDecimal minAmount) {
        Map<String, Object> method = createPaymentMethod(code, name, type, enabled, null);
        method.put("minTenure", minTenure);
        method.put("maxTenure", maxTenure);
        method.put("minAmount", minAmount);
        return method;
    }
    
    private boolean isValidLuhn(String cardNumber) {
        int sum = 0;
        boolean alternate = false;
        for (int i = cardNumber.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(cardNumber.charAt(i));
            if (alternate) {
                digit *= 2;
                if (digit > 9)
                    digit = (digit % 10) + 1;
            }
            sum += digit;
            alternate = !alternate;
        }
        return sum % 10 == 0;
    }
    
    private String detectCardBrand(String cardNumber) {
        if (cardNumber.startsWith("4")) return "VISA";
        if (cardNumber.startsWith("5") || cardNumber.matches("^2[2-7].*")) return "MASTERCARD";
        if (cardNumber.matches("^3[47].*"))
            return "AMERICAN_EXPRESS";
        if (cardNumber.matches("^(508|60).*")) return "RUPAY";
        return "UNKNOWN";
    }
}
