package com.eshop.app.admin.api.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Allow-listed set of fields an admin may update on a Keycloak-managed user.
 *
 * <p>Deliberately narrow: only fields legitimately safe for an admin to change via this
 * endpoint are exposed here. This replaces an open {@code Map<String, Object>} that
 * forwarded arbitrary caller-supplied fields directly to the Keycloak Admin API
 * (a mass-assignment vector — CWE-915). Extend this DTO field-by-field as genuine admin
 * needs are identified; never widen it back to an open map.</p>
 *
 * <p>All fields are optional (null = leave unchanged) to support partial updates.</p>
 */
@Data
@Schema(description = "Admin update request for a Keycloak-managed user (partial update; null fields are left unchanged)")
public class AdminUserUpdateRequest {

    @Size(max = 100, message = "First name must not exceed 100 characters")
    @Schema(description = "User's first name")
    private String firstName;

    @Size(max = 100, message = "Last name must not exceed 100 characters")
    @Schema(description = "User's last name")
    private String lastName;

    @Schema(description = "Whether the account is enabled")
    private Boolean enabled;

    @Schema(description = "Whether the user's email is marked as verified")
    private Boolean emailVerified;
}
