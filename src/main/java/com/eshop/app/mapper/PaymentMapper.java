package com.eshop.app.mapper;

import com.eshop.app.dto.response.PageResponse;
import com.eshop.app.dto.response.PaymentResponse;
import com.eshop.app.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.function.Function;
import java.util.List;

/**
 * Payment mapper for entity-DTO conversions
 * Optimized for performance with minimal object creation
 */
@Component
public class PaymentMapper {
    
    /**
     * Convert Payment entity to PaymentResponse DTO
     */
    public PaymentResponse toResponse(Payment payment) {
        if (payment == null) {
            return null;
        }
        
        return PaymentResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrder().getId())
                .transactionId(payment.getTransactionId())
                .gateway(payment.getGateway())
                .gatewayTransactionId(payment.getGatewayTransactionId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .paymentMethod(payment.getPaymentMethod())
                .failureReason(payment.getFailureReason())
                .processedAt(payment.getProcessedAt())
                .refundedAmount(payment.getRefundedAmount())
                .isRefunded(payment.getIsRefunded())
                
                // Card payment details
                .cardLastFour(payment.getCardLastFour())
                .cardBrand(payment.getCardBrand())
                .cardType(payment.getCardType())
                .cardIssuerBank(payment.getCardIssuerBank())
                .cardCountry(payment.getCardCountry())
                .isInternationalCard(payment.getIsInternationalCard())
                
                // UPI payment details
                .upiId(maskUpiId(payment.getUpiId()))
                .upiReferenceId(payment.getUpiReferenceId())
                .bankReferenceNumber(payment.getBankReferenceNumber())
                
                // EMI details
                .emiTenureMonths(payment.getEmiTenureMonths())
                .emiAmountPerMonth(payment.getEmiAmountPerMonth())
                .emiInterestRate(payment.getEmiInterestRate())
                
                // Security information
                .authenticationMethod(payment.getAuthenticationMethod())
                .riskScore(payment.getRiskScore())
                
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
    
    /**
     * Mask UPI ID for security (show only first 3 and last 3 characters)
     */
    private String maskUpiId(String upiId) {
        if (upiId == null || upiId.length() <= 6) {
            return upiId;
        }
        
        String[] parts = upiId.split("@");
        if (parts.length == 2) {
            String username = parts[0];
            String domain = parts[1];
            
            if (username.length() > 6) {
                String masked = username.substring(0, 3) + "***" + username.substring(username.length() - 3);
                return masked + "@" + domain;
            }
        }
        
        return upiId;
    }
    
    /**
     * Convert Page of Payment entities to PageResponse of PaymentResponse DTOs
     */
    public static <T, R> PageResponse<R> toPageResponse(Page<T> page, Function<T, R> mapper) {
        List<R> data = page.getContent().stream().map(mapper).toList();
        PageResponse.PageMetadata metadata = PageResponse.PageMetadata.builder()
            .page(page.getNumber())
            .size(page.getSize())
            .totalElements(page.getTotalElements())
            .totalPages(page.getTotalPages())
            .hasNext(page.hasNext())
            .hasPrevious(page.hasPrevious())
            .build();
        return PageResponse.<R>builder()
            .data(data)
            .pagination(metadata)
            .build();
    }
}