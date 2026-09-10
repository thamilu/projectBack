package com.eshop.app.realtime.infrastructure.config;

import com.corundumstudio.socketio.AuthorizationResult;
import com.corundumstudio.socketio.HandshakeData;
import com.corundumstudio.socketio.SocketConfig;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.store.RedissonStoreFactory;
import com.eshop.app.realtime.application.service.WsTicketService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Embedded Netty-based socket.io-protocol-compatible server.
 *
 * <p>Runs on its own port, independent of the main Spring MVC servlet
 * container — chosen over native Spring WebSocket/STOMP specifically to keep
 * the frontend's existing {@code socket.io-client} usage and channel-naming
 * scheme (see {@code ChannelSubscriptionHandler}) intact.
 *
 * <p>Uses a Redis-backed store ({@link RedissonStoreFactory}) rather than the
 * library's in-memory default, since this backend already assumes horizontal
 * scaling (see ShedLock usage elsewhere) — domain events published via {@code
 * ApplicationEventPublisher} are process-local, so without a shared store a
 * client connected to instance A would never receive a push published by a
 * listener running on instance B.
 */
@Configuration
@Slf4j
public class SocketIOServerConfig {

    @Value("${app.realtime.socketio.port:8090}")
    private int port;

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();
        String address = "redis://" + redisHost + ":" + redisPort;
        var single = config.useSingleServer().setAddress(address);
        if (redisPassword != null && !redisPassword.isBlank()) {
            single.setPassword(redisPassword);
        }
        return Redisson.create(config);
    }

    @Bean(destroyMethod = "stop")
    public SocketIOServer socketIOServer(WsTicketService wsTicketService, RedissonClient redissonClient) {
        com.corundumstudio.socketio.Configuration config = new com.corundumstudio.socketio.Configuration();
        config.setHostname("0.0.0.0");
        config.setPort(port);
        config.setOrigin(null); // CORS handled at the reverse-proxy layer, same as REST
        config.setStoreFactory(new RedissonStoreFactory(redissonClient));

        SocketConfig socketConfig = new SocketConfig();
        socketConfig.setReuseAddress(true);
        config.setSocketConfig(socketConfig);

        config.setAuthorizationListener(handshakeData -> validateHandshake(handshakeData, wsTicketService));

        return new SocketIOServer(config);
    }

    /**
     * Validates the connection ticket carried on the handshake.
     *
     * <p><b>Needs verification against a real client/server pair, not just
     * read-review</b>: socket.io-client's {@code auth} option (used by
     * {@code websocket-client.ts}) is sent as part of the Engine.IO handshake
     * and is readable here via {@link HandshakeData#getSingleUrlParam}
     * under the commonly-documented netty-socketio integration pattern —
     * but the exact extraction call can differ across socket.io protocol
     * versions and netty-socketio releases. If a real handshake doesn't
     * carry the ticket in the URL params the way this expects, check
     * {@code HandshakeData}'s available accessors on the actual installed
     * netty-socketio version and adjust the line below — everything else in
     * this class (ticket validation itself, single-use claim, fail-closed
     * behavior) is independent of exactly how the raw string is extracted.
     */
    private AuthorizationResult validateHandshake(
            HandshakeData handshakeData, WsTicketService wsTicketService) {
        String ticket = handshakeData.getSingleUrlParam("ticket");
        if (ticket == null || ticket.isBlank()) {
            log.warn("WS handshake rejected: no ticket present");
            return AuthorizationResult.FAILED_AUTHORIZATION;
        }

        try {
            String userId = wsTicketService.validateAndClaim(ticket);
            // Stash the resolved userId in the handshake data so the
            // @OnConnect handler (ChannelSubscriptionHandler) can attach it
            // to the session without re-parsing the ticket.
            handshakeData.getUrlParams().put("resolvedUserId", java.util.List.of(userId));
            return AuthorizationResult.SUCCESSFUL_AUTHORIZATION;
        } catch (Exception e) {
            log.warn("WS handshake rejected: ticket validation failed — {}", e.getMessage());
            return AuthorizationResult.FAILED_AUTHORIZATION;
        }
    }

    /**
     * Starts the socket.io server after the Spring context is fully up.
     * Deliberately not started inside the {@code @Bean} factory method
     * itself, to keep bean creation free of side effects (standard Spring
     * convention — a bean definition should be safely re-creatable/proxyable
     * without starting a network listener as a side effect).
     */
    @Bean
    public ApplicationRunner socketIOServerStarter(SocketIOServer socketIOServer) {
        return (ApplicationArguments args) -> {
            socketIOServer.start();
            log.info("WebSocket (socket.io) server started on port {}", port);
        };
    }
}
