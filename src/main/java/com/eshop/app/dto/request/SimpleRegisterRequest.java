package com.eshop.app.dto.request;

import com.eshop.app.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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
@Schema(description = "Simple user registration request")
public class SimpleRegisterRequest {
    
    @NotBlank(message = "Username is required")
    @Schema(description = "Username", example = "john_doe")
    private String username;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Schema(description = "Email address", example = "john@example.com")
    private String email;
    
    @NotBlank(message = "Password is required")
    @Schema(description = "Password", example = "password123")
    private String password;
    
    @NotBlank(message = "First name is required")
    @Schema(description = "First name", example = "John")
    private String firstName;
    
    @NotBlank(message = "Last name is required")
    @Schema(description = "Last name", example = "Doe")
    private String lastName;
    
    @NotBlank(message = "Phone is required")
    @Schema(description = "Phone number", example = "+1234567890")
    @JsonAlias("mobileNumber")
    private String phone;
    
    @Schema(description = "Address", example = "123 Main St")
    private String address;
    
    @NotNull(message = "Role is required")
    @Schema(description = "User role", example = "CUSTOMER")
    private Role role;
}