package com.eshop.app.catalog.infrastructure.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AttributeEncryptorTest {

    private final AttributeEncryptor encryptor = new AttributeEncryptor("test-encryption-key-for-unit-tests");

    @Test
    void roundTripsPlaintextThroughEncryptDecrypt() {
        String plaintext = "seller-bank-account-1234567890";

        String encrypted = encryptor.convertToDatabaseColumn(plaintext);
        String decrypted = encryptor.convertToEntityAttribute(encrypted);

        assertEquals(plaintext, decrypted);
    }

    @Test
    void producesDifferentCiphertextForIdenticalPlaintextAcrossCalls() {
        // Guards against regressing to ECB (or any deterministic/IV-less mode), which would
        // make identical plaintext blocks always encrypt to identical ciphertext.
        String plaintext = "repeated-value";

        String first = encryptor.convertToDatabaseColumn(plaintext);
        String second = encryptor.convertToDatabaseColumn(plaintext);

        assertNotEquals(first, second);
        assertEquals(plaintext, encryptor.convertToEntityAttribute(first));
        assertEquals(plaintext, encryptor.convertToEntityAttribute(second));
    }

    @Test
    void returnsNullForNullInput() {
        assertNull(encryptor.convertToDatabaseColumn(null));
        assertNull(encryptor.convertToEntityAttribute(null));
    }

    @Test
    void throwsInsteadOfReturningCorruptedDataOnDecryptFailure() {
        assertThrows(
                IllegalStateException.class,
                () -> encryptor.convertToEntityAttribute("not-valid-base64-ciphertext!!"));
    }

    @Test
    void refusesToConstructWithoutAnEncryptionKey() {
        assertThrows(IllegalStateException.class, () -> new AttributeEncryptor(""));
        assertThrows(IllegalStateException.class, () -> new AttributeEncryptor(null));
    }
}
