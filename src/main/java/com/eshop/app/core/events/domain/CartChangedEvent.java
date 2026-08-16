package com.eshop.app.core.events.domain;

import java.time.LocalDateTime;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Domain event published when an authenticated user's cart changes —
 * supports multi-device cart sync via a {@code cart:{userId}} WebSocket
 * push. Anonymous ({@code cartCode}-based, no owning user) cart mutations
 * never publish this event; there is no device to sync to.
 *
 * @see com.eshop.app.core.events.listener.CartEventListener
 */
@Getter
public class CartChangedEvent extends ApplicationEvent {

    public enum Action {
        ITEM_ADDED,
        ITEM_REMOVED,
        ITEM_UPDATED,
        CART_CLEARED
    }

    private final Long userId;
    private final Action action;
    private final LocalDateTime eventTimestamp;

    public CartChangedEvent(Object source, Long userId, Action action) {
        super(source);
        this.userId = userId;
        this.action = action;
        this.eventTimestamp = LocalDateTime.now();
    }
}
