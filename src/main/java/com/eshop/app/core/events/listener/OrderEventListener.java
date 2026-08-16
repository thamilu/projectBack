package com.eshop.app.core.events.listener;

import com.eshop.app.core.events.domain.OrderStatusChangedEvent;
import com.eshop.app.notification.application.service.NotificationService;
import com.eshop.app.realtime.infrastructure.socket.WsPushService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listener for order-related domain events.
 *
 * <p>Follows the same shape as {@code ProductEventListener}: {@code @Async}
 * and runs after the originating transaction commits, so a side-effect
 * failure here can never roll back the primary order operation. MDC
 * propagation follows {@code UserEventListener}'s pattern.
 *
 * <p>Two side effects per status change: push a live WebSocket update to
 * {@code order:{orderId}:status_changed}, and send the order confirmation
 * email — wiring up {@code NotificationService.sendOrderConfirmation}, which
 * previously had zero call sites anywhere in the codebase.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OrderEventListener {

    private static final String MDC_KEY_ORDER_ID = "orderId";
    private static final String MDC_KEY_ORDER_NUMBER = "orderNumber";

    private final WsPushService wsPushService;
    private final NotificationService notificationService;

    @Async("websocketExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderStatusChanged(OrderStatusChangedEvent event) {
        Map<String, String> parentMdc = populateTracingMdc(event.getOrderId(), event.getOrderNumber());
        try {
            log.info(
                    "Order status changed: id={}, number={}, {} -> {}",
                    event.getOrderId(),
                    event.getOrderNumber(),
                    event.getPreviousStatus(),
                    event.getNewStatus());

            wsPushService.push(
                    "order:" + event.getOrderId(),
                    "status_changed",
                    new OrderStatusPushPayload(
                            event.getOrderId(),
                            event.getOrderNumber(),
                            event.getPreviousStatus().name(),
                            event.getNewStatus().name(),
                            event.getEventTimestamp().toString()));

            if (event.getCustomerEmail() != null && !event.getCustomerEmail().isBlank()) {
                notificationService.sendOrderConfirmation(
                        event.getOrderNumber(),
                        event.getCustomerEmail(),
                        event.getTotalAmount(),
                        event.getNewStatus().name());
            }
        } catch (Exception e) {
            log.error(
                    "Failed to process order status change side effects for orderId={}",
                    event.getOrderId(),
                    e);
        } finally {
            restoreMdc(parentMdc);
        }
    }

    private Map<String, String> populateTracingMdc(Long orderId, String orderNumber) {
        Map<String, String> parentMdc = MDC.getCopyOfContextMap();
        MDC.put(MDC_KEY_ORDER_ID, String.valueOf(orderId));
        MDC.put(MDC_KEY_ORDER_NUMBER, orderNumber == null ? "unknown" : orderNumber);
        return parentMdc;
    }

    private void restoreMdc(Map<String, String> parentMdc) {
        MDC.clear();
        if (parentMdc != null && !parentMdc.isEmpty()) {
            MDC.setContextMap(parentMdc);
        }
    }

    /** Shape pushed to WebSocket subscribers — intentionally distinct from any REST DTO. */
    public record OrderStatusPushPayload(
            Long orderId, String orderNumber, String previousStatus, String newStatus, String occurredAt) {}
}
