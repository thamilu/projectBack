package com.eshop.app.seller.application.mapper;

import static org.junit.jupiter.api.Assertions.*;

import com.eshop.app.seller.api.response.SellerBusinessDetailsResponse;
import com.eshop.app.seller.api.response.SellerFarmerDetailsResponse;
import com.eshop.app.seller.api.response.SellerKYCResponse;
import com.eshop.app.seller.api.response.SellerWholesaleConfigResponse;
import com.eshop.app.seller.domain.entity.SellerBusinessDetails;
import com.eshop.app.seller.domain.entity.SellerFarmerDetails;
import com.eshop.app.seller.domain.entity.SellerKYC;
import com.eshop.app.seller.domain.entity.SellerWholesaleConfig;
import com.eshop.app.user.api.response.SellerProfileResponse;
import com.eshop.app.user.domain.entity.SellerProfile;
import com.eshop.app.user.domain.entity.User;
import com.eshop.app.user.domain.entity.UserProfile;
import com.eshop.app.seller.shared.domain.enums.SellerStatus;
import com.eshop.app.seller.shared.domain.enums.SellerIdentityType;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import com.eshop.app.seller.domain.entity.SellerBankAccount;
import com.eshop.app.seller.domain.entity.SellerDocument;
import com.eshop.app.seller.api.response.SellerBankAccountResponse;
import com.eshop.app.seller.api.response.SellerDocumentResponse;
import com.eshop.app.seller.api.request.SellerRegisterRequest;
import java.util.Set;
import java.util.List;

class SellerMapperTest {

    private final SellerMapper mapper = Mappers.getMapper(SellerMapper.class);

    private void setId(Object entity, Long id) {
        try {
            java.lang.reflect.Field field = entity.getClass().getSuperclass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) {
            try {
                java.lang.reflect.Field field = entity.getClass().getDeclaredField("id");
                field.setAccessible(true);
                field.set(entity, id);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    @Test
    void toResponse_withValidProfile_mapsFieldsCorrectly() {
        UserProfile userProfile = UserProfile.builder()
                .firstName("John")
                .lastName("Doe")
                .phone("1234567890")
                .build();
        User user = User.builder()
                .email("john@example.com")
                .userProfile(userProfile)
                .build();
        setId(user, 1L);

        SellerProfile profile = SellerProfile.builder()
                .user(user)
                .shopName("John's Shop")
                .city("New York")
                .identityType(SellerIdentityType.INDIVIDUAL)
                .status(SellerStatus.PENDING)
                .build();
        setId(profile, 10L);

        SellerProfileResponse response = mapper.toResponse(profile);

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals(1L, response.getUserId());
        assertEquals("john@example.com", response.getEmail());
        assertEquals("John", response.getFirstName());
        assertEquals("Doe", response.getLastName());
        assertEquals("John's Shop (New York)", response.getDisplayIdentity());
        assertEquals(com.eshop.app.seller.shared.domain.enums.SellerIdentityType.INDIVIDUAL, response.getIdentityType());
    }

    @Test
    void toKycResponse_mapsFieldsCorrectly() {
        SellerKYC kyc = SellerKYC.builder()
                .gstin("22AAAAA0000A1Z5")
                .build();
        setId(kyc, 1L);

        SellerKYCResponse response = mapper.toKycResponse(kyc);
        assertNotNull(response);
        assertEquals("22AAAAA0000A1Z5", response.getGstin());
    }

    @Test
    void toKycResponse_shouldMaskPanNumber() {
        SellerKYC kyc = SellerKYC.builder()
                .panNumber("ABCDE1234F")
                .build();
        setId(kyc, 1L);

        SellerKYCResponse response = mapper.toKycResponse(kyc);
        assertNotNull(response);
        assertEquals("XXXXX1234F", response.getPanNumber());
    }

    @Test
    void toKycResponse_shouldMaskAadharNumber() {
        SellerKYC kyc = SellerKYC.builder()
                .aadhar("123456781234")
                .build();
        setId(kyc, 1L);

        SellerKYCResponse response = mapper.toKycResponse(kyc);
        assertNotNull(response);
        assertEquals("XXXX-XXXX-1234", response.getAadhar());
    }

    @Test
    void toFarmerResponse_mapsFieldsCorrectly() {
        SellerFarmerDetails details = SellerFarmerDetails.builder()
                .landArea("10 Acres")
                .build();
        setId(details, 1L);

        SellerFarmerDetailsResponse response = mapper.toFarmerResponse(details);
        assertNotNull(response);
        assertEquals("10 Acres", response.getLandArea());
    }

    @Test
    void toBusinessResponse_mapsFieldsCorrectly() {
        SellerBusinessDetails details = SellerBusinessDetails.builder()
                .legalBusinessName("ACME Corp")
                .build();
        setId(details, 1L);

        SellerBusinessDetailsResponse response = mapper.toBusinessResponse(details);
        assertNotNull(response);
        assertEquals("ACME Corp", response.getLegalBusinessName());
    }

    @Test
    void toWholesaleResponse_mapsFieldsCorrectly() {
        SellerWholesaleConfig config = SellerWholesaleConfig.builder()
                .minOrderQuantity(10)
                .build();
        setId(config, 1L);

        SellerWholesaleConfigResponse response = mapper.toWholesaleResponse(config);
        assertNotNull(response);
        assertEquals(10, response.getMinOrderQuantity());
    }

    @Test
    void toBankAccountResponseList_shouldSortByIdAsc() {
        SellerBankAccount ba1 = SellerBankAccount.builder().bankName("Bank A").build();
        setId(ba1, 2L);
        SellerBankAccount ba2 = SellerBankAccount.builder().bankName("Bank B").build();
        setId(ba2, 1L);

        List<SellerBankAccountResponse> responses = mapper.toBankAccountResponseList(Set.of(ba1, ba2));
        assertNotNull(responses);
        assertEquals(2, responses.size());
        assertEquals(1L, responses.get(0).getId());
        assertEquals(2L, responses.get(1).getId());
    }

    @Test
    void toDocumentResponseList_shouldSortByIdAsc() {
        SellerDocument doc1 = SellerDocument.builder().documentUrl("url1").build();
        setId(doc1, 20L);
        SellerDocument doc2 = SellerDocument.builder().documentUrl("url2").build();
        setId(doc2, 10L);

        List<SellerDocumentResponse> responses = mapper.toDocumentResponseList(Set.of(doc1, doc2));
        assertNotNull(responses);
        assertEquals(2, responses.size());
        assertEquals(10L, responses.get(0).getId());
        assertEquals(20L, responses.get(1).getId());
    }

    @Test
    void updateProfileFromRequest_shouldIgnoreNulls() {
        SellerProfile profile = SellerProfile.builder()
                .shopName("Existing Shop")
                .shopHandle("existing-handle")
                .status(SellerStatus.ACTIVE)
                .build();

        SellerRegisterRequest request = new SellerRegisterRequest();
        // businessPhone and shopName are null in request

        mapper.updateProfileFromRequest(request, profile);

        assertEquals("Existing Shop", profile.getShopName());
        assertEquals("existing-handle", profile.getShopHandle());
        // status is mapped to PENDING as a constant if request isn't completely ignored
        assertEquals(SellerStatus.PENDING, profile.getStatus());
    }
}
