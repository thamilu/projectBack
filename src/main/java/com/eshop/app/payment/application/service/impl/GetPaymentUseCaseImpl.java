package com.eshop.app.payment.application.service.impl;

import com.eshop.app.payment.api.response.PaymentResponse;
import com.eshop.app.payment.application.mapper.PaymentMapper;
import com.eshop.app.payment.application.port.in.GetPaymentUseCase;
import com.eshop.app.payment.domain.entity.Payment;
import com.eshop.app.payment.domain.repository.PaymentRepository;
import com.eshop.app.payment.domain.model.PaymentStatus;
import com.eshop.app.core.api.response.PageResponse;
import com.eshop.app.core.exception.business.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GetPaymentUseCaseImpl implements GetPaymentUseCase {

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;

    @Override
    public PaymentResponse getPaymentByTransactionId(String transactionId) {
        return paymentRepository.findByTransactionId(transactionId)
                .map(paymentMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + transactionId));
    }

    @Override
    public List<PaymentResponse> getPaymentsByOrderId(Long orderId) {
        return paymentRepository.findByOrderIdOrderByCreatedAtDesc(orderId).stream()
                .map(paymentMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public PageResponse<PaymentResponse> getUserPayments(Long userId, Pageable pageable) {
        Page<Payment> page = paymentRepository.findByUserId(userId, pageable);
        return PageResponse.of(page, paymentMapper::toResponse);
    }

    @Override
    public PageResponse<PaymentResponse> getPaymentsByStatus(PaymentStatus status, Pageable pageable) {
        Page<Payment> page = paymentRepository.findByStatus(status, pageable);
        return PageResponse.of(page, paymentMapper::toResponse);
    }

    @Override
    public List<PaymentResponse> getFailedPaymentsForRetry() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(1);
        return paymentRepository.findFailedPaymentsSince(PaymentStatus.FAILED, cutoff).stream()
                .map(paymentMapper::toResponse)
                .collect(Collectors.toList());
    }
}




