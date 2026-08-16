package com.eshop.app.user.infrastructure.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class LogMaskingUtilsTest {

    @Test
    void testMaskEmail() {
        assertEquals("[null]", LogMaskingUtils.maskEmail(null));
        assertEquals("adm***@eshop.com", LogMaskingUtils.maskEmail("admin@eshop.com"));
        assertEquals("tes***@example.co.uk", LogMaskingUtils.maskEmail("test@example.co.uk"));
        assertEquals("[masked]", LogMaskingUtils.maskEmail("invalid_email"));
    }

    @Test
    void testMaskId() {
        assertEquals("[null]", LogMaskingUtils.maskId(null));
        assertEquals("keyc****6789", LogMaskingUtils.maskId("keycloak-12345-6789"));
        assertEquals("****", LogMaskingUtils.maskId("short"));
    }
}
