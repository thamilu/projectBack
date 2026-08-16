package com.eshop.app.seller.api.response;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class SellerAggregationMetricsDTOTest {

    private final ObjectMapper objectMapper =
            new ObjectMapper()
                    .setVisibility(
                            com.fasterxml.jackson.annotation.PropertyAccessor.FIELD,
                            com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY);

    @Test
    void builder_defaultValues_shouldBeEnforced() {
        SellerAggregationMetricsDTO dto =
                SellerAggregationMetricsDTO.builder()
                        .todaySales(BigDecimal.valueOf(100.25))
                        .build();

        assertNotNull(dto);
        assertEquals(BigDecimal.valueOf(100.25), dto.getTodaySales());
        assertNull(dto.getWeeklySales());
        assertEquals(0L, dto.getNewOrders());
        assertEquals(0L, dto.getProcessingOrders());
        assertEquals(0L, dto.getShippedOrders());
        assertEquals(0L, dto.getCompletedOrders());
    }

    @Test
    void serialization_shouldFormatBigDecimalAndOmitNulls() throws Exception {
        SellerAggregationMetricsDTO dto =
                SellerAggregationMetricsDTO.builder()
                        .todaySales(new BigDecimal("123.4"))
                        .weeklySales(new BigDecimal("5000.1234"))
                        .newOrders(5L)
                        .build();

        String json = objectMapper.writeValueAsString(dto);
        System.out.println("JSON OUTPUT IS: " + json);

        // Assert 2 decimal formatting
        assertTrue(json.contains("\"todaySales\":\"123.40\""), "Should format with 2 decimals");
        assertTrue(json.contains("\"weeklySales\":\"5000.12\""), "Should format with 2 decimals");

        // Assert json property naming
        assertTrue(json.contains("\"newOrders\":5"), "Should use newOrders field name");

        // Assert null exclusion via @JsonInclude(NON_NULL)
        assertFalse(
                json.contains("monthlySales"), "Null values should be excluded from serialization");
        assertFalse(
                json.contains("totalSales"), "Null values should be excluded from serialization");
    }
}
