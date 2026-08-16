package com.eshop.app.core.events.listener;

import com.eshop.app.core.events.domain.CartChangedEvent;
import com.eshop.app.realtime.infrastructure.socket.WsPushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listener for cart-change domain events — pushes {@code cart:{userId}}
 * WebSocket updates for multi-device cart sync. Same {@code @Async
 * @TransactionalEventListener(AFTER_COMMIT)} shape as {@code
 * ProductEventListener}/{@code OrderEventListener}.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CartEventListener {

    private final WsPushService wsPushService;

    @Async("websocketExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCartChanged(CartChangedEvent event) {
        if (event.getUserId() == null) {
            // Anonymous (cartCode-based) cart mutation — no owning user, no device to sync to.
            return;
        }

        log.debug("Cart changed: userId={}, action={}", event.getUserId(), event.getAction());

        wsPushService.push(
                "cart:" + event.getUserId(),
                "updated",
                new CartUpdatePushPayload(
                        event.getUserId(), event.getAction().name(), event.getEventTimestamp().toString()));
    }

    public record CartUpdatePushPayload(Long userId, String action, String occurredAt) {}
}
