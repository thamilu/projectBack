package com.eshop.app.user.application.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eshop.app.core.exception.business.DomainValidationException;
import com.eshop.app.user.application.service.ProfileSyncCommand;
import com.eshop.app.user.domain.entity.User;
import com.eshop.app.user.domain.enums.Gender;
import com.eshop.app.user.shared.domain.enums.UserRole;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class DefaultProfileSyncServiceTest {

    private final com.eshop.app.user.domain.repository.UserRepository userRepository =
            org.mockito.Mockito.mock(com.eshop.app.user.domain.repository.UserRepository.class);
    private final DefaultProfileSyncService service = new DefaultProfileSyncService(userRepository);

    private User createTestUser() {
        return User.create(
                "12345678-1234-1234-1234-123456789012", "test@example.com", UserRole.CUSTOMER);
    }

    @Test
    void ensureProfileExists_withValidDisplayName_shouldParseGender() {
        User user = createTestUser();

        service.ensureProfileExists(
                user,
                new ProfileSyncCommand(
                        "John",
                        "Doe",
                        "9876543210",
                        "9876543211",
                        "Prefer Not to Say",
                        "en",
                        LocalDate.of(1990, 1, 1)));

        assertThat(user.getUserProfile()).isNotNull();
        assertThat(user.getUserProfile().getGender()).isEqualTo(Gender.PREFER_NOT_TO_SAY);
    }

    @Test
    void ensureProfileExists_withValidEnumName_shouldParseGender() {
        User user = createTestUser();

        service.ensureProfileExists(
                user,
                new ProfileSyncCommand(
                        "John",
                        "Doe",
                        "9876543210",
                        "9876543211",
                        "PREFER_NOT_TO_SAY",
                        "en",
                        LocalDate.of(1990, 1, 1)));

        assertThat(user.getUserProfile().getGender()).isEqualTo(Gender.PREFER_NOT_TO_SAY);
    }

    @Test
    void ensureProfileExists_withValidCode_shouldParseGender() {
        User user = createTestUser();

        service.ensureProfileExists(
                user,
                new ProfileSyncCommand(
                        "John",
                        "Doe",
                        "9876543210",
                        "9876543211",
                        "X",
                        "en",
                        LocalDate.of(1990, 1, 1)));

        assertThat(user.getUserProfile().getGender()).isEqualTo(Gender.PREFER_NOT_TO_SAY);
    }

    @Test
    void ensureProfileExists_withInvalidGender_shouldThrowDomainValidationException() {
        User user = createTestUser();

        assertThatThrownBy(
                        () ->
                                service.ensureProfileExists(
                                        user,
                                        new ProfileSyncCommand(
                                                "John",
                                                "Doe",
                                                "9876543210",
                                                "9876543211",
                                                "InvalidGenderOption",
                                                "en",
                                                LocalDate.of(1990, 1, 1))))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("Invalid gender option: InvalidGenderOption");
    }
}

