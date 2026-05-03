package com.eshop.app.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonAlias;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSelfUpdateRequest {

    @Size(max = 100)
    private String firstName;

    @Size(max = 100)
    private String lastName;

    @Size(max = 20)
    @JsonAlias("mobileNumber")
    private String phone;

    @Size(max = 255)
    private String address;

    @Size(max = 500)
    @com.fasterxml.jackson.annotation.JsonAlias("address_line1")
    private String addressLine1;

    @Size(max = 500)
    @com.fasterxml.jackson.annotation.JsonAlias("address_line2")
    private String addressLine2;

    @Size(max = 100)
    private String city;

    @Size(max = 100)
    private String district;

    @Size(max = 100)
    private String state;

    @Size(max = 20)
    private String pincode;

    @Size(max = 100)
    private String country;

    private java.time.LocalDate dateOfBirth;

    @Size(max = 20)
    private String gender;

}
