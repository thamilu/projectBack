package com.eshop.app.entity;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class UserProfileTest {

    @Test
    void userProfile_Properties_SetAndGetCorrectly() {
        UserProfile profile = UserProfile.builder()
                .firstName("John")
                .lastName("Doe")
                .phone("1234567890")
                .gender("Male")
                .preferredLanguage("English")
                .build();
        
        assertThat(profile.getFirstName()).isEqualTo("John");
        assertThat(profile.getLastName()).isEqualTo("Doe");
        assertThat(profile.getPhone()).isEqualTo("1234567890");
        assertThat(profile.getGender()).isEqualTo("Male");
        assertThat(profile.getPreferredLanguage()).isEqualTo("English");
    }

    @Test
    void userProfile_TrimmingNames_ShouldBeHandledByCaller() {
        // Since updateDisplayName was removed, we verify that names are stored as provided
        UserProfile profile = new UserProfile();
        profile.setFirstName(" John ");
        profile.setLastName(" Doe ");
        
        assertThat(profile.getFirstName()).isEqualTo(" John ");
        assertThat(profile.getLastName()).isEqualTo(" Doe ");
    }
}
