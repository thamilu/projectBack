package com.eshop.app.realtime.infrastructure.socket;

import com.corundumstudio.socketio.SocketIOServer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Single, shared entry point for pushing an event to every client subscribed
 * to a channel/room. Every domain-event listener that pushes a WebSocket
 * update goes through this class rather than calling {@link SocketIOServer}
 * directly, so the "how do we push" concern lives in exactly one place.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WsPushService {

    private final SocketIOServer socketIOServer;

    /**
     * Pushes {@code payload} as {@code eventName} to every client currently
     * subscribed to {@code channel}. Best-effort: a push failure must never
     * propagate back into the caller's (already-committed) business
     * transaction — this is always called from an {@code @Async
     * @TransactionalEventListener(AFTER_COMMIT)} handler, never inline.
     */
    public void push(String channel, String eventName, Object payload) {
        try {
            socketIOServer.getRoomOperations(channel).sendEvent(eventName, payload);
        } catch (Exception e) {
            log.warn("Failed to push WS event '{}' to channel '{}': {}", eventName, channel, e.getMessage());
        }
    }
}
