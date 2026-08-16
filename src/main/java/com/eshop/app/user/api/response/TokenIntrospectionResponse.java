package com.eshop.app.user.api.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Token introspection response metadata")
public class TokenIntrospectionResponse {

    @Schema(
            description = "Indicates whether or not the presented token is currently active",
            example = "true")
    private Boolean active;

    @Schema(
            description =
                    "A JSON string containing a space-separated list of scopes associated with this"
                            + " token",
            example = "openid profile email")
    private String scope;

    @Schema(description = "Client identifier for the token", example = "eshop-client")
    private String clientId;

    @Schema(
            description =
                    "Human-readable identifier for the resource owner who authorized this token",
            example = "john.doe@example.com")
    private String username;

    @Schema(description = "Type of the token", example = "Bearer")
    private String tokenType;

    @Schema(
            description =
                    "Integer timestamp, measured in seconds since the Epoch, indicating when this"
                            + " token will expire",
            example = "1774880000")
    private Long exp;

    @Schema(
            description =
                    "Integer timestamp, measured in seconds since the Epoch, indicating when this"
                            + " token was originally issued",
            example = "1774876400")
    private Long iat;

    @Schema(
            description =
                    "Integer timestamp, measured in seconds since the Epoch, indicating when this"
                            + " token is not to be used before",
            example = "1774876400")
    private Long nbf;

    @Schema(
            description = "Subject of the token, usually the user ID",
            example = "d3b07384-d113-4956-a5db-8f244199c9c8")
    private String sub;

    @Schema(description = "Audience for the token", example = "account")
    private String aud;

    @Schema(
            description = "Issuer of the token",
            example = "https://keycloak.eshop.com/realms/eshop")
    private String iss;

    @Schema(
            description = "Identifier for the token",
            example = "4e18d6ee-5c24-4f05-b049-8fa9449f864e")
    private String jti;

    @Schema(
            description =
                    "Server timestamp indicating when this introspection request was evaluated")
    private Instant timestamp;
}
