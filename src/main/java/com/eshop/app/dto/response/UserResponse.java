package com.eshop.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private Long id;
    private String keycloakId;
    private String username;
    private String email;
    private Boolean emailVerified;
    private String firstName;
    private String lastName;
    private String phone;
    private String address; // Keeps existing
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String district;
    private String state;
    private String country;
    private String pincode;

    // Demographics and KYC
    private String gender;
    private java.time.LocalDate dateOfBirth;

    private String role;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private StoreInfoResponse shop;
}
