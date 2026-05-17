package com.eshop.app.payment.application.service.impl;

import com.eshop.app.payment.application.port.in.PaymentAnalyticsUseCase;
import com.eshop.app.payment.domain.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PaymentAnalyticsUseCaseImpl implements PaymentAnalyticsUseCase {

    private final PaymentRepository paymentRepository;

    @Override
    public Object getPaymentStatistics(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null) startDate = LocalDateTime.now().minusDays(30);
        if (endDate == null) endDate = LocalDateTime.now();
        
        return paymentRepository.calculateRevenueBetween(startDate, endDate);
    }

    @Override
    public BigDecimal calculateRevenue(LocalDateTime startDate, LocalDateTime endDate) {
        return paymentRepository.calculateRevenueBetween(startDate, endDate);
    }
}
