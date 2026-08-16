package com.eshop.app.seller.api.response;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SellerAnalyticsResponseTest {

    private final ObjectMapper objectMapper =
            new ObjectMapper()
                    .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
                    .setVisibility(
                            com.fasterxml.jackson.annotation.PropertyAccessor.FIELD,
                            com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY);

    @Test
    void serialization_shouldFormatMonetaryValuesAndOmitNulls() throws Exception {
        // Arrange
        SellerAnalyticsResponse.SalesTrendData trendData =
                SellerAnalyticsResponse.SalesTrendData.builder()
                        .period("Week 24")
                        .revenue(new BigDecimal("12500.5"))
                        .orderCount(42L)
                        .build();

        SellerAnalyticsResponse.ProductPerformanceData productData =
                SellerAnalyticsResponse.ProductPerformanceData.builder()
                        .productId(1025L)
                        .productName("Premium Headphones")
                        .views(1500L)
                        .sales(150L)
                        .conversionRate(new BigDecimal("0.1000"))
                        .build();

        SellerAnalyticsResponse response =
                SellerAnalyticsResponse.builder()
                        .salesTrend(List.of(trendData))
                        .productPerformance(List.of(productData))
                        .revenueBreakdown(
                                Map.of(
                                        "credit_card",
                                        new BigDecimal("12500.5"),
                                        "paypal",
                                        new BigDecimal("4500.223")))
                        .generatedAt(Instant.parse("2026-07-03T12:00:00Z"))
                        .build();

        // Act
        String json = objectMapper.writeValueAsString(response);

        // Assert
        // 1. Assert @MonetaryField on SalesTrendData.revenue formats to 2 decimal places as a
        // String
        assertTrue(
                json.contains("\"revenue\":\"12500.50\""),
                "SalesTrendData revenue should be formatted as string with 2 decimal places");

        // 2. Assert @JsonSerialize(contentUsing = MoneySerializer.class) on Map values formats map
        // values to 2 decimal places as String
        assertTrue(
                json.contains("\"credit_card\":\"12500.50\""),
                "Map values should be formatted as string with 2 decimal places");
        assertTrue(
                json.contains("\"paypal\":\"4500.22\""),
                "Map values should be formatted as string with 2 decimal places and rounded"
                        + " HALF_UP");

        // 3. Assert conversionRate is serialized normally as a numeric decimal (since it's a ratio,
        // not a monetary value)
        assertTrue(
                json.contains("\"conversionRate\":0.1000"),
                "conversionRate should be serialized as a raw number without string formatting");

        // 4. Assert null exclusion via @JsonInclude(NON_NULL)
        assertFalse(
                json.contains("customerDemographics"),
                "Null values should be excluded from serialization");
    }
}
