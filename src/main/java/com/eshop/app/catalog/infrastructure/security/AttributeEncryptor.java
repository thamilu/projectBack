package com.eshop.app.catalog.infrastructure.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * JPA Attribute Converter for transparent AES-256-GCM encryption at rest.
 * Ensures PII data is encrypted before persisting to the database.
 *
 * Mandatory for Compliance with Enterprise Standards (Rule 189).
 *
 * <p>Uses AES/GCM with a random 96-bit IV per value (authenticated encryption — provides both
 * confidentiality and integrity, unlike ECB which leaks plaintext patterns). The IV is stored
 * alongside the ciphertext (prefixed) since it is not secret and must be available to decrypt.
 */
@Slf4j
@Component("catalogAttributeEncryptor")
@Converter
public class AttributeEncryptor implements AttributeConverter<String, String> {

    private static final String CIPHER_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String KEY_ALGORITHM = "AES";
    private static final int GCM_IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;

    private final SecretKeySpec secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public AttributeEncryptor(@Value("${app.security.encryption-key:}") String key) {
        if (!StringUtils.hasText(key)) {
            throw new IllegalStateException(
                    "app.security.encryption-key must be configured — refusing to start "
                            + "without a real PII encryption key");
        }
        this.secretKey = new SecretKeySpec(deriveKey(key), KEY_ALGORITHM);
    }

    /** Derives a fixed-length AES-256 key from an arbitrary-length passphrase via SHA-256. */
    private static byte[] deriveKey(String passphrase) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(passphrase.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable — cannot derive encryption key", e);
        }
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] ciphertext = cipher.doFinal(attribute.getBytes(StandardCharsets.UTF_8));

            byte[] ivAndCiphertext = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, ivAndCiphertext, 0, iv.length);
            System.arraycopy(ciphertext, 0, ivAndCiphertext, iv.length, ciphertext.length);

            return Base64.getEncoder().encodeToString(ivAndCiphertext);
        } catch (Exception e) {
            log.error("Error during encryption of database column", e);
            throw new IllegalStateException("Could not encrypt database column", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            byte[] ivAndCiphertext = Base64.getDecoder().decode(dbData);
            if (ivAndCiphertext.length < GCM_IV_LENGTH_BYTES) {
                throw new IllegalArgumentException("Encrypted value is shorter than the IV length");
            }
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            byte[] ciphertext = new byte[ivAndCiphertext.length - GCM_IV_LENGTH_BYTES];
            System.arraycopy(ivAndCiphertext, 0, iv, 0, GCM_IV_LENGTH_BYTES);
            System.arraycopy(ivAndCiphertext, GCM_IV_LENGTH_BYTES, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception e) {
            // Fail loudly: silently returning undecryptable/corrupted ciphertext as plaintext
            // would leak encrypted blobs into business logic, API responses, and logs.
            log.error("Error during decryption of database column", e);
            throw new IllegalStateException("Could not decrypt database column", e);
        }
    }
}
