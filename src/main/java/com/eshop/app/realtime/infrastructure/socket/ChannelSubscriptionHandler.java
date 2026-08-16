package com.eshop.app.realtime.infrastructure.socket;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.annotation.OnConnect;
import com.corundumstudio.socketio.annotation.OnDisconnect;
import com.corundumstudio.socketio.annotation.OnEvent;
import com.eshop.app.core.infrastructure.config.security.oauth.PrincipalDetails;
import com.eshop.app.core.infrastructure.config.security.web.UserSecurityExpression;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Handles per-channel subscribe/unsubscribe authorization for WebSocket
 * clients, and attaches the ticket-resolved userId to each session.
 *
 * <p>The connection ticket only proves "who this is" (see {@code
 * WsTicketService}) — it deliberately carries no channel-specific claims.
 * Channel-level authorization ("what is this user allowed to see") is
 * re-checked here, at subscribe time, against the database — the same
 * approach the REST API uses via {@code UserSecurityExpression}, reused
 * directly rather than reimplemented, so the two authorization paths can't
 * drift apart.
 *
 * <p><b>Known scoping limitation</b>: the ticket carries only a userId, no
 * role claims, so the {@link SecurityContext} constructed here has no
 * granted authorities — {@code canViewOrder}'s {@code isAdmin()} branch is
 * always false in this context. An admin viewing another user's order via
 * the regular authenticated REST API is unaffected; they simply won't
 * receive real-time WebSocket pushes for orders they don't own or fulfill.
 * This fails closed (denies), not open, and is an intentional scope
 * boundary for this pass rather than an oversight.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ChannelSubscriptionHandler {

    private final SocketIOServer socketIOServer;
    private final UserSecurityExpression userSecurity;

    @PostConstruct
    public void registerListeners() {
        socketIOServer.addConnectListener(this::onConnect);
        socketIOServer.addDisconnectListener(this::onDisconnect);
        socketIOServer.addEventListener("subscribe", ChannelRequest.class, this::onSubscribe);
        socketIOServer.addEventListener("unsubscribe", ChannelRequest.class, this::onUnsubscribe);
    }

    private void onConnect(SocketIOClient client) {
        String userId = extractResolvedUserId(client);
        if (userId == null) {
            log.warn("WS client connected without a resolved userId — disconnecting");
            client.disconnect();
            return;
        }
        client.set("userId", userId);
        log.info("WS client connected, sessionId={}, userId={}", client.getSessionId(), userId);
    }

    private void onDisconnect(SocketIOClient client) {
        log.info("WS client disconnected, sessionId={}", client.getSessionId());
    }

    private void onSubscribe(SocketIOClient client, ChannelRequest request, com.corundumstudio.socketio.AckRequest ack) {
        if (!isAuthorizedForChannel(client, request.channel())) {
            log.warn(
                    "WS subscribe denied: sessionId={}, channel={}",
                    client.getSessionId(),
                    request.channel());
            return;
        }
        client.joinRoom(request.channel());
        log.debug("WS client subscribed: sessionId={}, channel={}", client.getSessionId(), request.channel());
    }

    private void onUnsubscribe(SocketIOClient client, ChannelRequest request, com.corundumstudio.socketio.AckRequest ack) {
        client.leaveRoom(request.channel());
    }

    /**
     * Authorizes a channel subscription per the channel-prefix rules:
     * {@code order:{orderId}} requires order-view permission (owner, order's
     * store/seller, or admin); {@code cart:{userId}}/{@code user:{userId}}
     * require the channel's userId to match the session's authenticated
     * userId; {@code product:{productId}} is public (matches {@code GET
     * /api/v1/products/**}'s permitAll()).
     */
    private boolean isAuthorizedForChannel(SocketIOClient client, String channel) {
        if (channel == null || channel.isBlank()) return false;

        String sessionUserId = client.get("userId");
        if (sessionUserId == null) return false;

        String[] parts = channel.split(":");
        if (parts.length < 2) return false;

        String prefix = parts[0];
        String resourceId = parts[1];

        return switch (prefix) {
            case "product" -> true; // public data, same as GET /products/**
            case "cart", "user" -> resourceId.equals(sessionUserId);
            case "order" -> canViewOrder(sessionUserId, resourceId);
            default -> {
                log.warn("WS subscribe to unknown channel prefix: {}", prefix);
                yield false;
            }
        };
    }

    private boolean canViewOrder(String sessionUserId, String orderIdRaw) {
        Long orderId;
        try {
            orderId = Long.valueOf(orderIdRaw);
        } catch (NumberFormatException e) {
            return false;
        }

        SecurityContext previous = SecurityContextHolder.getContext();
        try {
            SecurityContextHolder.setContext(buildSecurityContext(sessionUserId));
            return userSecurity.canViewOrder(orderId);
        } finally {
            // Always restore, never leave a fabricated SecurityContext on a
            // pooled worker thread that netty-socketio may reuse for
            // unrelated clients' events.
            SecurityContextHolder.setContext(previous);
        }
    }

    private SecurityContext buildSecurityContext(String userId) {
        PrincipalDetails principal = PrincipalDetails.builder().id(Long.valueOf(userId)).build();
        Authentication authentication =
                new UsernamePasswordAuthenticationToken(principal, null, List.of());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        return context;
    }

    private String extractResolvedUserId(SocketIOClient client) {
        Map<String, List<String>> urlParams = client.getHandshakeData().getUrlParams();
        List<String> values = urlParams.get("resolvedUserId");
        return (values == null || values.isEmpty()) ? null : values.get(0);
    }

    public record ChannelRequest(String channel) {}
}
