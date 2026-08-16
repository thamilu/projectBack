package com.eshop.app.user.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eshop.app.core.exception.business.DomainValidationException;
import com.eshop.app.location.domain.entity.Country;
import com.eshop.app.location.domain.entity.District;
import com.eshop.app.location.domain.entity.PostalCode;
import com.eshop.app.location.domain.entity.State;
import org.junit.jupiter.api.Test;

class UserAddressTest {

    @Test
    void create_EnforcesInvariantsAndSetsFields() {
        UserProfile profile = mock(UserProfile.class);

        UserAddress address =
                UserAddress.create(
                        profile,
                        "123 Main St",
                        "Apt 4B",
                        "Metropolis",
                        "Metro District",
                        "Metro Taluk",
                        "Metro State",
                        "12345",
                        "Metro Country",
                        true);

        assertThat(address.getUserProfile()).isEqualTo(profile);
        assertThat(address.getAddressLine1()).isEqualTo("123 Main St");
        assertThat(address.getAddressLine2()).isEqualTo("Apt 4B");
        assertThat(address.getCity()).isEqualTo("Metropolis");
        assertThat(address.getDistrict()).isEqualTo("Metro District");
        assertThat(address.getTaluk()).isEqualTo("Metro Taluk");
        assertThat(address.getState()).isEqualTo("Metro State");
        assertThat(address.getPincode()).isEqualTo("12345");
        assertThat(address.getCountry()).isEqualTo("Metro Country");
        assertThat(address.getIsDefault()).isTrue();
        assertThat(address.isDefaultAddress()).isTrue();
    }

    @Test
    void create_ThrowsOnNullUserProfile() {
        assertThatThrownBy(
                        () ->
                                UserAddress.create(
                                        null,
                                        "123 Main St",
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        false))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("userProfile");
    }

    @Test
    void create_DefaultsIsDefaultToFalseWhenNull() {
        UserProfile profile = mock(UserProfile.class);
        UserAddress address =
                UserAddress.create(profile, "Addr", null, null, null, null, null, null, null, null);

        assertThat(address.getIsDefault()).isFalse();
        assertThat(address.isDefaultAddress()).isFalse();
    }

    @Test
    void updateCoordinates_ValidatesLatitudeRange() {
        UserProfile profile = mock(UserProfile.class);
        UserAddress address =
                UserAddress.create(
                        profile, "123 Main St", null, null, null, null, null, null, null, false);

        // Valid
        address.updateCoordinates(45.0, 90.0);
        assertThat(address.getLatitude()).isEqualTo(45.0);
        assertThat(address.getLongitude()).isEqualTo(90.0);
        assertThat(address.hasCoordinates()).isTrue();

        // Invalid latitude — too high
        assertThatThrownBy(() -> address.updateCoordinates(95.0, 0.0))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("latitude");

        // Invalid latitude — too low
        assertThatThrownBy(() -> address.updateCoordinates(-90.1, 0.0))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("latitude");
    }

    @Test
    void updateCoordinates_ValidatesLongitudeRange() {
        UserProfile profile = mock(UserProfile.class);
        UserAddress address =
                UserAddress.create(
                        profile, "123 Main St", null, null, null, null, null, null, null, false);

        // Invalid longitude — too high
        assertThatThrownBy(() -> address.updateCoordinates(0.0, 180.1))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("longitude");

        // Invalid longitude — too low
        assertThatThrownBy(() -> address.updateCoordinates(0.0, -180.5))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("longitude");
    }

    @Test
    void updateCoordinates_AcceptsNulls() {
        UserProfile profile = mock(UserProfile.class);
        UserAddress address =
                UserAddress.create(
                        profile, "123 Main St", null, null, null, null, null, null, null, false);

        address.updateCoordinates(null, null);
        assertThat(address.getLatitude()).isNull();
        assertThat(address.getLongitude()).isNull();
        assertThat(address.hasCoordinates()).isFalse();
    }

    @Test
    void assignPostalCode_SyncsFields() {
        UserProfile profile = mock(UserProfile.class);
        UserAddress address =
                UserAddress.create(
                        profile, "123 Main St", null, null, null, null, null, null, null, false);

        PostalCode postalCode = mock(PostalCode.class);
        State state = mock(State.class);
        District district = mock(District.class);
        Country country = mock(Country.class);

        when(postalCode.getPinCode()).thenReturn("600001");
        when(postalCode.getPostOfficeName()).thenReturn("Chennai GPO");
        when(postalCode.getState()).thenReturn(state);
        when(state.getName()).thenReturn("Tamil Nadu");
        when(postalCode.getDistrict()).thenReturn(district);
        when(district.getName()).thenReturn("Chennai");
        when(postalCode.getCountry()).thenReturn(country);
        when(country.getName()).thenReturn("India");

        address.assignPostalCode(postalCode);

        assertThat(address.getPincode()).isEqualTo("600001");
        assertThat(address.getCity()).isEqualTo("Chennai GPO");
        assertThat(address.getState()).isEqualTo("Tamil Nadu");
        assertThat(address.getDistrict()).isEqualTo("Chennai");
        assertThat(address.getCountry()).isEqualTo("India");
        assertThat(address.getPostalCode()).isEqualTo(postalCode);
        assertThat(address.hasPostalCode()).isTrue();
    }

    @Test
    void updateAddressDetails_EnforcesAddressLineLengthLimits() {
        UserProfile profile = mock(UserProfile.class);
        UserAddress address =
                UserAddress.create(
                        profile, "123 Main St", null, null, null, null, null, null, null, false);

        String tooLongAddressLine = "a".repeat(501);

        assertThatThrownBy(
                        () ->
                                address.updateAddressDetails(
                                        tooLongAddressLine,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("addressLine1");
    }

    @Test
    void updateAddressDetails_EnforcesTextFieldLengthLimits() {
        UserProfile profile = mock(UserProfile.class);
        UserAddress address =
                UserAddress.create(
                        profile, "123 Main St", null, null, null, null, null, null, null, false);

        String tooLongTextField = "a".repeat(101);

        assertThatThrownBy(
                        () ->
                                address.updateAddressDetails(
                                        null, null, tooLongTextField, null, null, null, null, null))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("city");
    }

    @Test
    void updateAddressDetails_EnforcesPincodeLengthLimits() {
        UserProfile profile = mock(UserProfile.class);
        UserAddress address =
                UserAddress.create(
                        profile, "123 Main St", null, null, null, null, null, null, null, false);

        String tooLongPincode = "a".repeat(21);

        assertThatThrownBy(
                        () ->
                                address.updateAddressDetails(
                                        null, null, null, null, null, null, tooLongPincode, null))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("pincode");
    }

    @Test
    void defaultStatus_MutatesCorrectly() {
        UserProfile profile = mock(UserProfile.class);
        UserAddress address =
                UserAddress.create(
                        profile, "123 Main St", null, null, null, null, null, null, null, false);

        assertThat(address.getIsDefault()).isFalse();
        assertThat(address.isDefaultAddress()).isFalse();

        address.markAsDefault();
        assertThat(address.getIsDefault()).isTrue();
        assertThat(address.isDefaultAddress()).isTrue();

        address.unmarkAsDefault();
        assertThat(address.getIsDefault()).isFalse();
        assertThat(address.isDefaultAddress()).isFalse();
    }

    @Test
    void queryMethods_ReturnCorrectState() {
        UserProfile profile = mock(UserProfile.class);
        UserAddress address =
                UserAddress.create(
                        profile, "123 Main St", null, null, null, null, null, null, null, false);

        // No coordinates initially
        assertThat(address.hasCoordinates()).isFalse();

        // After setting coordinates
        address.updateCoordinates(12.9716, 77.5946);
        assertThat(address.hasCoordinates()).isTrue();

        // No postal code initially
        assertThat(address.hasPostalCode()).isFalse();

        // After assigning postal code
        PostalCode postalCode = mock(PostalCode.class);
        when(postalCode.getPinCode()).thenReturn("560001");
        when(postalCode.getPostOfficeName()).thenReturn("Bangalore GPO");
        address.assignPostalCode(postalCode);
        assertThat(address.hasPostalCode()).isTrue();
    }
}
