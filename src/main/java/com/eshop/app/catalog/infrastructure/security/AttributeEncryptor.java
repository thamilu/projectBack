package com.eshop.app.catalog.infrastructure.security;





import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 * JPA Attribute Converter for transparent AES-256 encryption at rest.
 * Ensures PII data is encrypted before persisting to the database.
 * 
 * Mandatory for Compliance with Enterprise Standards (Rule 189).
 */
@Slf4j
@Component
@Converter
public class AttributeEncryptor implements AttributeConverter<String, String> {

    private static final String AES = "AES";
    private final SecretKeySpec secretKey;

    public AttributeEncryptor(@Value("${app.security.encryption-key:default-encryption-key-32chars!!}") String key) {
        // Ensure key is exactly 16, 24, or 32 bytes for AES
        byte[] keyBytes = key.getBytes();
        byte[] finalKey = new byte[32];
        System.arraycopy(keyBytes, 0, finalKey, 0, Math.min(keyBytes.length, 32));
        this.secretKey = new SecretKeySpec(finalKey, AES);
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            Cipher cipher = Cipher.getInstance(AES);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return Base64.getEncoder().encodeToString(cipher.doFinal(attribute.getBytes()));
        } catch (Exception e) {
            log.error("Error during encryption of database column", e);
            throw new RuntimeException("Could not encrypt database column", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            Cipher cipher = Cipher.getInstance(AES);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            return new String(cipher.doFinal(Base64.getDecoder().decode(dbData)));
        } catch (Exception e) {
            log.error("Error during decryption of database column", e);
            // Fallback: if decryption fails, return raw data (assumes migration phase)
            // Or throw exception depending on security policy
            return dbData;
        }
    }
}
