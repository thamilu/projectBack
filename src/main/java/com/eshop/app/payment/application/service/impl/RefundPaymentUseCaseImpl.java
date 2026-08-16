package com.eshop.app.payment.application.service.impl;

import com.eshop.app.payment.api.request.RefundRequest;
import com.eshop.app.payment.api.response.PaymentResponse;
import com.eshop.app.payment.application.mapper.PaymentMapper;
import com.eshop.app.payment.application.port.in.RefundPaymentUseCase;
import com.eshop.app.payment.application.service.PaymentGatewayResult;
import com.eshop.app.payment.application.service.PaymentGatewayService;
import com.eshop.app.payment.domain.entity.Payment;
import com.eshop.app.payment.domain.repository.PaymentRepository;
import com.eshop.app.payment.domain.model.PaymentStatus;
import com.eshop.app.payment.shared.exception.PaymentException;
import com.eshop.app.core.exception.business.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class RefundPaymentUseCaseImpl implements RefundPaymentUseCase {

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final PaymentGatewayService paymentGatewayService;

    @Override
    public PaymentResponse processRefund(RefundRequest request) {
        log.info("Processing refund for payment: {}", request.getPaymentId());

        Payment payment = paymentRepository.findById(request.getPaymentId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + request.getPaymentId()));

        if (!payment.canBeRefunded()) {
            throw new PaymentException("Payment cannot be refunded");
        }

        if (request.getAmount().compareTo(payment.getRefundableAmount()) > 0) {
            throw new PaymentException("Refund amount exceeds refundable amount");
        }

        PaymentGatewayResult result =
                paymentGatewayService.refund(payment, request.getAmount(), request.getReason());

        if (!result.isSuccess()) {
            log.warn(
                    "Gateway refund failed for payment {}: {}", payment.getId(), result.getMessage());
            throw new PaymentException("Refund failed: " + result.getMessage());
        }

        BigDecimal currentRefunded = payment.getRefundedAmount() != null ? payment.getRefundedAmount() : BigDecimal.ZERO;
        payment.setRefundedAmount(currentRefunded.add(request.getAmount()));

        if (payment.getRefundedAmount().compareTo(payment.getAmount()) >= 0) {
            payment.setStatus(PaymentStatus.REFUNDED);
            payment.setIsRefunded(true);
        } else {
            payment.setStatus(PaymentStatus.PARTIALLY_REFUNDED);
        }

        payment = paymentRepository.save(payment);
        log.info(
                "Refund {} processed via gateway for payment {}",
                result.getGatewayTransactionId(),
                payment.getId());
        return paymentMapper.toResponse(payment);
    }
}


