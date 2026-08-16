package com.eshop.app.payment.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Single, reusable implementation of the HMAC-based signature schemes used by payment gateway
 * webhooks (Razorpay, Cashfree, PayU) and Stripe's timestamped variant.
 *
 * <p>Every verification here is fail-closed: a blank/missing secret or signature is treated as
 * invalid rather than skipped, and all comparisons are constant-time
 * ({@link MessageDigest#isEqual}) to avoid leaking timing information about how much of the
 * signature matched.
 */
@Component
@Slf4j
public class PaymentSignatureVerifier {

    /** Stripe's documented replay-attack tolerance window. */
    private static final long STRIPE_TOLERANCE_SECONDS = 300;

    /**
     * Verifies a plain HMAC-SHA256 hex-encoded signature (Razorpay, Cashfree style: {@code
     * hex(hmac_sha256(payload, secret))}).
     */
    public boolean verifyHmacSha256Hex(String payload, String signature, String secret) {
        if (!StringUtils.hasText(secret)) {
            log.warn("Webhook signature verification skipped-as-invalid: no secret configured");
            return false;
        }
        if (!StringUtils.hasText(signature) || !StringUtils.hasText(payload)) {
            return false;
        }
        String expected = hmacHex(payload, secret, "HmacSHA256");
        return constantTimeEquals(expected, signature);
    }

    /**
     * Verifies a Stripe-style {@code Stripe-Signature} header: {@code t=<timestamp>,v1=<hex hmac>}
     * signed over {@code "<timestamp>.<payload>"}, rejecting signatures older than the replay
     * tolerance window (Stripe's own documented webhook verification requirement).
     */
    public boolean verifyStripeStyleSignature(String payload, String signatureHeader, String secret) {
        if (!StringUtils.hasText(secret)) {
            log.warn("Stripe webhook signature verification skipped-as-invalid: no secret configured");
            return false;
        }
        if (!StringUtils.hasText(signatureHeader)) {
            return false;
        }

        String timestamp = null;
        String v1Signature = null;
        for (String part : signatureHeader.split(",")) {
            if (part.startsWith("t=")) {
                timestamp = part.substring(2);
            } else if (part.startsWith("v1=")) {
                v1Signature = part.substring(3);
            }
        }
        if (timestamp == null || v1Signature == null) {
            return false;
        }

        long timestampSeconds;
        try {
            timestampSeconds = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            return false;
        }
        long age = Instant.now().getEpochSecond() - timestampSeconds;
        if (Math.abs(age) > STRIPE_TOLERANCE_SECONDS) {
            log.warn("Stripe webhook rejected: timestamp outside tolerance window (age={}s)", age);
            return false;
        }

        String expected = hmacHex(timestamp + "." + payload, secret, "HmacSHA256");
        return constantTimeEquals(expected, v1Signature);
    }

    /**
     * Verifies a SHA-512 hex-encoded signature over an already gateway-formatted string (PayU's
     * response/webhook reverse-hash scheme: {@code sha512(salt|status|...|key)}, computed by the
     * caller and passed in as {@code signedString} since the exact field ordering is
     * gateway/merchant-config specific).
     */
    public boolean verifySha512Hex(String signedString, String signature, String secret) {
        if (!StringUtils.hasText(secret)) {
            log.warn("PayU webhook signature verification skipped-as-invalid: no salt configured");
            return false;
        }
        if (!StringUtils.hasText(signature) || !StringUtils.hasText(signedString)) {
            return false;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            byte[] hash = digest.digest(signedString.getBytes(StandardCharsets.UTF_8));
            String expected = bytesToHex(hash);
            return constantTimeEquals(expected, signature);
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-512 unavailable — cannot verify PayU signature", e);
            return false;
        }
    }

    private String hmacHex(String data, String secret, String algorithm) {
        try {
            Mac mac = Mac.getInstance(algorithm);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), algorithm));
            return bytesToHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Failed to compute {} signature", algorithm, e);
            return "";
        }
    }

    private boolean constantTimeEquals(String expected, String actual) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}
