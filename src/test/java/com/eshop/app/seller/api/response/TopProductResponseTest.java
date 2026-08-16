package com.eshop.app.seller.api.response;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class TopProductResponseTest {

    private final ObjectMapper objectMapper =
            new ObjectMapper()
                    .setVisibility(
                            com.fasterxml.jackson.annotation.PropertyAccessor.FIELD,
                            com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY);

    @Test
    void serialization_shouldFormatMonetaryValuesAndOmitNulls() throws Exception {
        // Arrange
        TopProductResponse response =
                TopProductResponse.builder()
                        .productId(1001L)
                        .productName("Organic Rice")
                        .sku("ORG-RICE")
                        .unitsSold(342L)
                        .totalRevenue(new BigDecimal("51300.5"))
                        .averagePrice(new BigDecimal("150.003"))
                        .revenueContributionPercent(23.5)
                        .build();

        // Act
        String json = objectMapper.writeValueAsString(response);

        // Assert
        // 1. Assert @MonetaryField formatting (exactly 2 decimal places, as a string)
        assertTrue(
                json.contains("\"totalRevenue\":\"51300.50\""),
                "totalRevenue should be formatted as string with 2 decimal places");
        assertTrue(
                json.contains("\"averagePrice\":\"150.00\""),
                "averagePrice should be formatted as string with 2 decimal places and rounded"
                        + " HALF_UP");

        // 2. Assert other fields are serialized normally
        assertTrue(
                json.contains("\"productId\":1001"), "productId should be serialized as a number");
        assertTrue(
                json.contains("\"productName\":\"Organic Rice\""),
                "productName should be serialized as a string");

        // 3. Assert null values are omitted (@JsonInclude(NON_NULL))
        assertFalse(
                json.contains("thumbnailUrl"), "Null values should be excluded from serialization");
        assertFalse(
                json.contains("categoryName"), "Null values should be excluded from serialization");
    }
}
