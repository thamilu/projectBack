package com.eshop.app.user.domain.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eshop.app.core.exception.business.DomainValidationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class EventValidationAndSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void testIsRecentValidationBounds() {
        TwoFactorEnabledEvent event = TwoFactorEnabledEvent.createForSystem(1L);

        // Positive bounds should not throw
        assertThat(event.isRecent(60)).isTrue();

        // Non-positive bounds must throw DomainValidationException without exposing the value
        assertThatThrownBy(() -> event.isRecent(0))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("Recency window must be positive")
                .hasMessageNotContaining("0");

        assertThatThrownBy(() -> event.isRecent(-10))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("Recency window must be positive")
                .hasMessageNotContaining("-10");
    }

    @Test
    void testEventValidatorIpValidation() {
        // Valid IPv4
        assertThat(EventValidator.validateIpAddress("127.0.0.1", "Required"))
                .isEqualTo("127.0.0.1");
        assertThat(EventValidator.validateIpAddress(" 192.168.1.1  ", "Required"))
                .isEqualTo("192.168.1.1");

        // Valid IPv6 forms
        assertThat(EventValidator.validateIpAddress("::1", "Required")).isEqualTo("::1");
        assertThat(EventValidator.validateIpAddress("2001:db8::1", "Required"))
                .isEqualTo("2001:db8::1");
        assertThat(EventValidator.validateIpAddress("[::1]", "Required")).isEqualTo("[::1]");

        // Invalid forms must throw and NOT expose the input IP address value in the error message
        assertThatThrownBy(() -> EventValidator.validateIpAddress("999.999.999.999", "Required"))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("IP address must be a valid IPv4 or IPv6 address")
                .hasMessageNotContaining("999.999.999.999");

        assertThatThrownBy(() -> EventValidator.validateIpAddress("invalid-ip", "Required"))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("IP address must be a valid IPv4 or IPv6 address")
                .hasMessageNotContaining("invalid-ip");

        assertThatThrownBy(() -> EventValidator.validateIpAddress("", "Required"))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("Required");
    }

    @Test
    void testSystemMigrationEnablementSource() {
        TwoFactorEnabledEvent event = TwoFactorEnabledEvent.createForSystem(42L);
        assertThat(event.getUserId()).isEqualTo(42L);
        assertThat(event.getEnablementSource())
                .isEqualTo(TwoFactorEnabledEvent.TwoFactorEnablementSource.SYSTEM_MIGRATION);
        assertThat(event.isSystemMigration()).isTrue();
    }

    @Test
    void testJacksonSerializationOfEnumGetters() throws Exception {
        TwoFactorEnabledEvent event = TwoFactorEnabledEvent.createForSystem(42L);
        String json = objectMapper.writeValueAsString(event);

        // Enums with getters must NOT serialize their labels/descriptions/etc. as nested objects.
        // They must be serialized as simple string names (e.g., "TOTP", "SYSTEM_MIGRATION").
        assertThat(json).contains("\"twoFactorMethod\":\"TOTP\"");
        assertThat(json).contains("\"enablementSource\":\"SYSTEM_MIGRATION\"");

        // Deserialization check
        TwoFactorEnabledEvent deserialized =
                objectMapper.readValue(json, TwoFactorEnabledEvent.class);
        assertThat(deserialized.getUserId()).isEqualTo(42L);
        assertThat(deserialized.getTwoFactorMethod())
                .isEqualTo(TwoFactorEnabledEvent.TwoFactorMethod.TOTP);
        assertThat(deserialized.getEnablementSource())
                .isEqualTo(TwoFactorEnabledEvent.TwoFactorEnablementSource.SYSTEM_MIGRATION);
    }
}
