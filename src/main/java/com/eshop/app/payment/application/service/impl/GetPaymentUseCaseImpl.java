package com.eshop.app.payment.application.service.impl;

import com.eshop.app.payment.api.response.PaymentResponse;
import com.eshop.app.payment.application.mapper.PaymentMapper;
import com.eshop.app.payment.application.port.in.GetPaymentUseCase;
import com.eshop.app.payment.domain.entity.Payment;
import com.eshop.app.payment.domain.repository.PaymentRepository;
import com.eshop.app.payment.domain.model.PaymentStatus;
import com.eshop.app.core.api.response.PageResponse;
import com.eshop.app.core.exception.business.ResourceNotFoundException;
import com.eshop.app.core.exception.security.UnauthorizedException;
import com.eshop.app.core.util.SecurityUtils;
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
        Payment payment = paymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + transactionId));
        verifyOwnership(payment);
        return paymentMapper.toResponse(payment);
    }

    @Override
    public List<PaymentResponse> getPaymentsByOrderId(Long orderId) {
        List<Payment> payments = paymentRepository.findByOrderIdOrderByCreatedAtDesc(orderId);
        if (!payments.isEmpty()) {
            verifyOwnership(payments.get(0));
        }
        return payments.stream()
                .map(paymentMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public PageResponse<PaymentResponse> getUserPayments(Long userId, Pageable pageable) {
        if (!SecurityUtils.hasRole("ADMIN")) {
            Long currentUserId = SecurityUtils.getAuthenticatedUserId();
            if (currentUserId == null || !currentUserId.equals(userId)) {
                throw new UnauthorizedException("You do not have permission to view this payment history");
            }
        }
        Page<Payment> page = paymentRepository.findByUserId(userId, pageable);
        return PageResponse.of(page, paymentMapper::toResponse);
    }

    /**
     * Payment ownership is resolved via the parent order's customer — {@code @PreAuthorize} on the
     * controller only checks role, not resource ownership. Without this, any authenticated
     * CUSTOMER could view any other customer's payment/card/transaction details by guessing a
     * sequential order ID.
     */
    private void verifyOwnership(Payment payment) {
        if (SecurityUtils.hasRole("ADMIN")) {
            return;
        }
        Long currentUserId = SecurityUtils.getAuthenticatedUserId();
        boolean isOwner = currentUserId != null
                && payment.getOrder() != null
                && payment.getOrder().getCustomer() != null
                && currentUserId.equals(payment.getOrder().getCustomer().getId());
        if (!isOwner) {
            throw new UnauthorizedException("You do not have permission to access this payment");
        }
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




