package com.eshop.app.dto.request;

import com.eshop.app.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonAlias;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User registration request with role selection")
public class RegisterRequest {
    
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 100, message = "Username must be between 3 and 100 characters")
    @Schema(description = "Unique username for login", example = "john_doe", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Size(max = 150, message = "Email must not exceed 150 characters")
    @Schema(description = "User email address", example = "john.doe@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;
    
    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    @Schema(description = "Account password (min 6 characters)", example = "SecurePass123!", requiredMode = Schema.RequiredMode.REQUIRED, format = "password")
    private String password;

    // confirmPassword is a client-only form field used for client-side validation
    // it is not required by the API (clients should strip it before sending)
    private String confirmPassword;
    
    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name must not exceed 100 characters")
    @Schema(description = "User's first name", example = "John", requiredMode = Schema.RequiredMode.REQUIRED)
    private String firstName;
    
    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name must not exceed 100 characters")
    @Schema(description = "User's last name", example = "Doe", requiredMode = Schema.RequiredMode.REQUIRED)
    private String lastName;
    
    @NotBlank(message = "Mobile number is required")
    @Size(max = 20, message = "Phone must not exceed 20 characters")
    @Schema(description = "Contact phone number", example = "+1-234-567-8900", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonAlias("mobileNumber")
    private String phone;
    
    @Schema(description = "Full address for delivery or contact", example = "123 Green Valley, Rural Route 5, Farmville")
    private String address;

    @NotNull(message = "Role is required")
    @Schema(description = "The role of the user in the system. Either `role` (enum) or `roleName` (string) may be provided.")
    private Role role;

    @Size(max = 50)
    @Schema(description = "Alternate role name (string) accepted from legacy JSON, e.g. 'SELLER' or 'ADMIN'")
    private String roleName;

    // Allow empty when not a SELLER; accept case-insensitive values when provided
    @Pattern(regexp = "(?i)^(INDIVIDUAL|BUSINESS|FARMER|WHOLESALER|RETAILER|RETAIL|RETAIL_SELLER|SHOP|SHOP_SELLER)?$", message = "Invalid seller type")
    @Schema(description = "Seller type when role is SELLER", example = "FARMER", allowableValues = {"INDIVIDUAL","BUSINESS","FARMER","WHOLESALER","RETAILER"})
    private String sellerType;

    // Optional seller/farmer specific fields
    @Size(max = 20)
    private String aadhar;

    @Size(max = 20)
    private String pan;

    @Size(max = 20)
    private String panNumber; // alternate incoming name

    @Size(max = 20)
    private String gstin;

    @Size(max = 20)
    private String gstinNumber; // alternate incoming name

    @Size(max = 100)
    private String businessType;

    @Size(max = 150)
    private String storeName;

    @Size(max = 150)
    private String businessName;

    @Size(max = 150)
    private String farmLocationVillage;

    @Size(max = 50)
    private String landArea;

    @Size(max = 255)
    @Schema(description = "Warehouse location for wholesaler sellers (optional)", example = "Plot 12, Zona Industrial")
    private String warehouseLocation;

    @Schema(description = "Indicates if seller has a bulk pricing agreement (optional)")
    private Boolean bulkPricingAgreement;

    // Alternate names from incoming JSON samples
    @Size(max = 20)
    @Schema(description = "Aadhar number (alternate field name: aadharNumber)")
    private String aadharNumber;

    @Size(max = 100)
    @Schema(description = "Farming land area (alternate field name: farmingLandArea)")
    private String farmingLandArea;

    @Size(max = 100)
    @Schema(description = "Vehicle type for delivery agents (e.g., Bike, Car)")
    private String vehicleType;
}
