package com.eshop.app.payment.application.port.in;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Use case for payment analytics and reporting.
 */
public interface PaymentAnalyticsUseCase {
    Object getPaymentStatistics(LocalDateTime startDate, LocalDateTime endDate);
    BigDecimal calculateRevenue(LocalDateTime startDate, LocalDateTime endDate);
}
