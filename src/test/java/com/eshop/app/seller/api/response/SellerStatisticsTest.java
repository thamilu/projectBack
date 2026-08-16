package com.eshop.app.seller.api.response;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class SellerStatisticsTest {

    private final ObjectMapper objectMapper =
            new ObjectMapper()
                    .setVisibility(
                            com.fasterxml.jackson.annotation.PropertyAccessor.FIELD,
                            com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY);

    @Test
    void serialization_shouldFormatMonetaryValuesAndOmitNulls() throws Exception {
        // Arrange
        SellerStatistics stats =
                SellerStatistics.builder()
                        .totalOrders(1250L)
                        .totalRevenue(new BigDecimal("52340.5"))
                        .pendingOrders(15L)
                        .completedOrders(1200L)
                        .cancelledOrders(35L)
                        .totalProducts(120L)
                        .activeProducts(95L)
                        .averageRating(4.75)
                        .totalCustomers(870L)
                        .monthlyRevenue(new BigDecimal("12500.003"))
                        .build();

        // Act
        String json = objectMapper.writeValueAsString(stats);

        // Assert
        // 1. Assert @MonetaryField formatting (exactly 2 decimal places, as a string)
        assertTrue(
                json.contains("\"totalRevenue\":\"52340.50\""),
                "totalRevenue should be formatted as string with 2 decimal places");
        assertTrue(
                json.contains("\"monthlyRevenue\":\"12500.00\""),
                "monthlyRevenue should be formatted as string with 2 decimal places and rounded"
                        + " HALF_UP");

        // 2. Assert other fields are serialized normally
        assertTrue(
                json.contains("\"totalOrders\":1250"),
                "totalOrders should be serialized as a number");
        assertTrue(
                json.contains("\"averageRating\":4.75"),
                "averageRating should be serialized as a number");

        // 3. Assert null values are omitted (@JsonInclude(NON_NULL))
        assertFalse(
                json.contains("monthlyOrders"),
                "Null values should be excluded from serialization");
    }
}
