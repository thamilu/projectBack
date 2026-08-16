package com.eshop.app.realtime.application.service;

import com.eshop.app.realtime.domain.exception.WsTicketException;
import com.eshop.app.realtime.infrastructure.config.WsTicketConfig;
import com.eshop.app.user.infrastructure.util.HmacUtil;
import com.eshop.app.user.infrastructure.util.NonceGenerator;
import com.eshop.app.user.infrastructure.util.SystemClock;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

/**
 * Mints and validates short-lived, single-use WebSocket connection tickets.
 *
 * <p>Structurally mirrors {@code OAuth2StateService}'s HMAC-signed,
 * time-bounded stateless token pattern (nonce | payload | timestamp |
 * signature, base64url-encoded) — reused here rather than introducing JWT
 * self-signing, since this token is opaque, backend-only-issued, and
 * backend-only-validated.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WsTicketService {

    private static final String REDIS_KEY_PREFIX = "ws:ticket:used:";

    private final WsTicketConfig config;
    private final SystemClock clock;
    private final NonceGenerator nonceGenerator;
    private final StringRedisTemplate redisTemplate;

    /** Mints a new ticket scoped to the given (local database) user id. */
    public String generateTicket(String userId) {
        String nonce = nonceGenerator.generate();
        long timestamp = clock.now().toEpochMilli();
        long ttlSeconds = config.getValidity().toSeconds();
        String payload = buildPayload(nonce, userId, timestamp, ttlSeconds);
        String signature = HmacUtil.hmacSha256(payload, config.getSigningKey());
        String ticket = payload + "|" + signature;
        return Base64.getUrlEncoder().encodeToString(ticket.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Validates a ticket and atomically claims it for single use.
     *
     * <p>Fails CLOSED on any Redis error — the deliberate opposite of
     * {@code IdempotencyKeyService}'s degrade-open behavior. That service
     * guards against duplicate request processing, where "assume first
     * execution" on a Redis outage is an acceptable default; this method
     * guards a live connection credential, where the same default would mean
     * a single-use ticket could be replayed indefinitely for the duration of
     * any Redis outage. An unreachable Redis must never be treated as "this
     * ticket is unused."
     *
     * @return the userId the ticket was minted for
     * @throws WsTicketException if the ticket is malformed, expired, tampered, or already used
     */
    public String validateAndClaim(String ticket) {
        TicketToken token = parseTicket(ticket);
        validateTimestamp(token.timestamp(), token.ttlSeconds());
        validateSignature(token);
        claimSingleUse(token.nonce(), token.ttlSeconds());
        return token.userId();
    }

    private TicketToken parseTicket(String ticket) {
        if (ticket == null || ticket.isBlank()) {
            throw new WsTicketException("Ticket is required");
        }
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(ticket);
            String decodedStr = new String(decoded, StandardCharsets.UTF_8);
            String[] parts = decodedStr.split("\\|");

            if (parts.length != 5) {
                throw new WsTicketException("Malformed ticket");
            }

            return new TicketToken(
                    parts[0], // nonce
                    parts[1], // userId
                    Long.parseLong(parts[2]), // timestamp
                    Long.parseLong(parts[3]), // ttlSeconds
                    parts[4] // signature
                    );
        } catch (WsTicketException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            throw new WsTicketException("Invalid ticket encoding");
        } catch (Exception e) {
            throw new WsTicketException("Failed to parse ticket");
        }
    }

    private void validateTimestamp(long timestamp, long ttlSeconds) {
        long now = clock.now().toEpochMilli();
        long age = now - timestamp;
        long validityMillis = Duration.ofSeconds(ttlSeconds).toMillis();

        if (age > validityMillis) {
            throw new WsTicketException("Ticket expired");
        }
        if (age < -config.getClockSkewTolerance().toMillis()) {
            throw new WsTicketException("Ticket timestamp in future (clock skew)");
        }
    }

    private void validateSignature(TicketToken token) {
        String payload =
                buildPayload(token.nonce(), token.userId(), token.timestamp(), token.ttlSeconds());
        String expected = HmacUtil.hmacSha256(payload, config.getSigningKey());

        // Constant-time comparison to prevent timing attacks
        if (!MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                token.signature().getBytes(StandardCharsets.UTF_8))) {
            log.error("WS ticket signature invalid — possible tampering. nonce=[{}]", token.nonce());
            throw new WsTicketException("Invalid ticket signature");
        }
    }

    /**
     * Atomically checks-and-claims the ticket's nonce in Redis so it can
     * never be redeemed twice (e.g. two connection attempts racing on the
     * same leaked ticket).
     */
    private void claimSingleUse(String nonce, long ttlSeconds) {
        String redisKey = REDIS_KEY_PREFIX + nonce;
        try {
            String luaScript =
                    """
                    if redis.call('EXISTS', KEYS[1]) == 1 then
                        return 0
                    end
                    redis.call('SET', KEYS[1], '1', 'EX', ARGV[1])
                    return 1
                    """;

            Long claimed =
                    redisTemplate.execute(
                            RedisScript.of(luaScript, Long.class),
                            List.of(redisKey),
                            String.valueOf(ttlSeconds));

            if (claimed == null || claimed != 1L) {
                log.warn("WS ticket replay attempt rejected. nonce=[{}]", nonce);
                throw new WsTicketException("Ticket already used");
            }
        } catch (WsTicketException e) {
            throw e;
        } catch (Exception e) {
            log.error("Redis unavailable during WS ticket claim — failing closed", e);
            throw new WsTicketException("Unable to verify ticket — please retry");
        }
    }

    private String buildPayload(String nonce, String userId, long timestamp, long ttlSeconds) {
        return String.format("%s|%s|%d|%d", nonce, userId, timestamp, ttlSeconds);
    }

    private record TicketToken(
            String nonce, String userId, long timestamp, long ttlSeconds, String signature) {}
}
