package com.eshop.app.payment.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.MessageDigest;
import java.time.Instant;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

class PaymentSignatureVerifierTest {

    private final PaymentSignatureVerifier verifier = new PaymentSignatureVerifier();

    // ── HMAC-SHA256 hex (Razorpay/Cashfree style) ──────────────────────────

    @Test
    void verifyHmacSha256Hex_acceptsCorrectSignature() {
        String payload = "{\"event\":\"payment.captured\"}";
        String secret = "webhook-secret";
        String signature = hmacHex(payload, secret, "HmacSHA256");

        assertTrue(verifier.verifyHmacSha256Hex(payload, signature, secret));
    }

    @Test
    void verifyHmacSha256Hex_rejectsTamperedPayload() {
        String secret = "webhook-secret";
        String signature = hmacHex("original-payload", secret, "HmacSHA256");

        assertFalse(verifier.verifyHmacSha256Hex("tampered-payload", signature, secret));
    }

    @Test
    void verifyHmacSha256Hex_failsClosedWhenSecretBlank() {
        String payload = "payload";
        String signature = hmacHex(payload, "some-secret", "HmacSHA256");

        assertFalse(verifier.verifyHmacSha256Hex(payload, signature, ""));
        assertFalse(verifier.verifyHmacSha256Hex(payload, signature, null));
    }

    @Test
    void verifyHmacSha256Hex_rejectsBlankSignature() {
        assertFalse(verifier.verifyHmacSha256Hex("payload", "", "secret"));
        assertFalse(verifier.verifyHmacSha256Hex("payload", null, "secret"));
    }

    // ── Stripe-style t=/v1= signature with replay tolerance ────────────────

    @Test
    void verifyStripeStyleSignature_acceptsFreshSignature() {
        String payload = "{\"type\":\"payment_intent.succeeded\"}";
        String secret = "whsec_test";
        long now = Instant.now().getEpochSecond();
        String signedPayload = now + "." + payload;
        String v1 = hmacHex(signedPayload, secret, "HmacSHA256");
        String header = "t=" + now + ",v1=" + v1;

        assertTrue(verifier.verifyStripeStyleSignature(payload, header, secret));
    }

    @Test
    void verifyStripeStyleSignature_rejectsExpiredTimestamp() {
        String payload = "{\"type\":\"payment_intent.succeeded\"}";
        String secret = "whsec_test";
        long tenMinutesAgo = Instant.now().getEpochSecond() - 600;
        String signedPayload = tenMinutesAgo + "." + payload;
        String v1 = hmacHex(signedPayload, secret, "HmacSHA256");
        String header = "t=" + tenMinutesAgo + ",v1=" + v1;

        assertFalse(verifier.verifyStripeStyleSignature(payload, header, secret));
    }

    @Test
    void verifyStripeStyleSignature_rejectsMalformedHeader() {
        assertFalse(verifier.verifyStripeStyleSignature("payload", "not-a-valid-header", "secret"));
    }

    @Test
    void verifyStripeStyleSignature_failsClosedWhenSecretBlank() {
        long now = Instant.now().getEpochSecond();
        assertFalse(
                verifier.verifyStripeStyleSignature(
                        "payload", "t=" + now + ",v1=abc123", ""));
    }

    // ── SHA-512 hex (PayU style) ────────────────────────────────────────────

    @Test
    void verifySha512Hex_acceptsCorrectSignature() throws NoSuchAlgorithmException {
        String signedString = "salt|success|||||||||email|firstname|productinfo|100.00|txn123|key";
        MessageDigest digest = MessageDigest.getInstance("SHA-512");
        String expected = bytesToHex(digest.digest(signedString.getBytes(StandardCharsets.UTF_8)));

        assertTrue(verifier.verifySha512Hex(signedString, expected, "salt"));
    }

    @Test
    void verifySha512Hex_rejectsIncorrectSignature() {
        assertFalse(verifier.verifySha512Hex("salt|success|txn123", "not-the-real-hash", "salt"));
    }

    @Test
    void verifySha512Hex_failsClosedWhenSecretBlank() {
        assertFalse(verifier.verifySha512Hex("salt|success|txn123", "somehash", ""));
    }

    private String hmacHex(String data, String secret, String algorithm) {
        try {
            Mac mac = Mac.getInstance(algorithm);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), algorithm));
            return bytesToHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException(e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}
