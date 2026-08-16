package com.eshop.app.seller.api.controller;

import com.eshop.app.core.api.response.ApiResponse;
import com.eshop.app.core.kernel.ApiConstants;
import com.eshop.app.seller.api.response.SellerBusinessTypeResponse;
import com.eshop.app.seller.api.response.SellerIdentityTypeResponse;
import com.eshop.app.seller.shared.domain.enums.SellerBusinessType;
import com.eshop.app.seller.shared.domain.enums.SellerIdentityType;
import com.eshop.app.user.application.service.SellerProfileService;
import io.micrometer.observation.annotation.Observed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Seller Lookup Controller — Provides public metadata and utility lookup endpoints.
 *
 * <p>Handles three distinct lookup categories:
 *
 * <ul>
 *   <li><strong>Handle availability:</strong> Real-time check against the seller profile store —
 *       requires authentication to prevent enumeration abuse.
 *   <li><strong>Identity types:</strong> Static enum metadata (INDIVIDUAL, BUSINESS) for
 *       registration form population — publicly accessible, aggressively cacheable.
 *   <li><strong>Business types:</strong> Static enum metadata (FARMER, RETAILER, etc.) for
 *       registration form population — publicly accessible, aggressively cacheable.
 * </ul>
 *
 * <p><strong>Performance note:</strong> Identity type and business type responses are pre-computed
 * as static constants at class load time. HTTP responses carry long-TTL {@code Cache-Control}
 * headers to maximize client-side and CDN caching.
 *
 * <p><strong>Authorization note:</strong> Handle availability requires authentication (prevent
 * enumeration). Identity and business type endpoints are publicly accessible as they are required
 * by the seller registration flow before authentication.
 */
@Tag(
        name = "Seller Lookup",
        description =
                "Seller metadata and utility lookups — Handle availability checks, identity"
                        + " types, and business type enumerations. Static metadata endpoints"
                        + " are publicly accessible and aggressively cached.")
@RestController
@RequestMapping(ApiConstants.BASE_PATH + "/sellers")
@RequiredArgsConstructor
@Validated
@Slf4j
public class SellerLookupController {

    private final SellerProfileService sellerProfileService;

    // =========================================================================
    // Static Metadata Constants
    // =========================================================================

    /**
     * Pre-computed identity type response list.
     *
     * <p>Initialized once at class load from {@link SellerIdentityType} enum values. This is safe
     * because enum constants are immutable and cannot change at runtime. Avoids stream allocation
     * on every HTTP request.
     *
     * <p>Content changes only on deployment — aligned with {@link #STATIC_METADATA_CACHE_TTL_DAYS}
     * HTTP cache TTL.
     */
    private static final List<SellerIdentityTypeResponse> IDENTITY_TYPES =
            Arrays.stream(SellerIdentityType.values())
                    .map(
                            type ->
                                    SellerIdentityTypeResponse.builder()
                                            .type(type.name())
                                            .label(type.getDisplayName())
                                            .description(type.getDescription())
                                            .build())
                    .toList();

    /**
     * Pre-computed business type response list.
     *
     * <p>Initialized once at class load from {@link SellerBusinessType} enum values. Same
     * immutability guarantee as {@link #IDENTITY_TYPES}.
     */
    private static final List<SellerBusinessTypeResponse> BUSINESS_TYPES =
            Arrays.stream(SellerBusinessType.values())
                    .map(
                            type ->
                                    SellerBusinessTypeResponse.builder()
                                            .type(type.name())
                                            .label(type.getDisplayName())
                                            .build())
                    .toList();

    /**
     * HTTP Cache-Control TTL for static enum-derived metadata (in days).
     *
     * <p>Identity and business type enumerations change only on deployment. A 1-day TTL allows CDN
     * and browser caches to serve these responses without hitting the origin, while ensuring stale
     * data is evicted within 24 hours of a deployment that changes enum values.
     *
     * <p>Increase to 7 days once enum stability is confirmed across releases.
     */
    private static final int STATIC_METADATA_CACHE_TTL_DAYS = 1;

    /**
     * Pre-built immutable {@link CacheControl} instance for static metadata endpoints.
     *
     * <p>Constructed once at class load — not per-request. {@code public()} allows CDN and proxy
     * caching in addition to browser caching, since identity/business type metadata is not
     * user-specific.
     */
    private static final CacheControl STATIC_METADATA_CACHE =
            CacheControl.maxAge(STATIC_METADATA_CACHE_TTL_DAYS, TimeUnit.DAYS)
                    .cachePublic()
                    .immutable();

    // =========================================================================
    // Handle Availability — Authenticated
    // =========================================================================

    /**
     * Check whether a given shop handle is available for registration.
     *
     * <p>This endpoint performs a live database lookup and requires authentication to prevent
     * unauthenticated enumeration of registered seller handles.
     *
     * @param handle the shop handle to check (lowercase letters, numbers, hyphens; 3–50 chars)
     * @return {@code true} if the handle is available; {@code false} if already taken
     */
    @GetMapping("/check-handle/{handle}")
    @PreAuthorize(
            "hasAnyRole(@appProperties.security.roles.customer,"
                    + " @appProperties.security.roles.seller,"
                    + " @appProperties.security.roles.admin)")
    @Observed(name = "seller.lookup.handle", contextualName = "seller-check-handle-availability")
    @Operation(
            summary = "Check shop handle availability",
            description =
                    """
                    Verify if a specific shop handle is available for registration.

                    **Validation Rules:**
                    - Length: 3–50 characters
                    - Allowed characters: lowercase letters (`a-z`), digits (`0-9`), hyphens (`-`)
                    - No leading or trailing hyphens
                    - Case-insensitive check (input is normalized to lowercase)

                    **Authorization:** Requires authentication. Unauthenticated handle
                    enumeration is not permitted to prevent seller data exposure.

                    **Returns:** `true` = available, `false` = already taken.
                    """,
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Handle availability result returned",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Bad Request — Handle fails validation constraints"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Unauthorized — Authentication required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "Forbidden — CUSTOMER, SELLER, or ADMIN role required")
    })
    public ResponseEntity<ApiResponse<Boolean>> checkHandle(
            @Parameter(
                            description =
                                    "Shop handle to check. Must be 3–50 characters,"
                                            + " lowercase letters, digits, and hyphens only.",
                            example = "green-valley-farm")
                    @PathVariable
                    @NotBlank
                    @Size(min = 3, max = 50)
                    @Pattern(
                            regexp = "^[a-z0-9][a-z0-9-]*[a-z0-9]$|^[a-z0-9]$",
                            message =
                                    "Shop handle must start and end with a letter or digit and"
                                            + " may contain only lowercase letters, numbers,"
                                            + " and hyphens.")
                    String handle) {

        boolean available =
                !sellerProfileService.existsByShopHandle(handle.toLowerCase(Locale.ROOT));
        log.debug("Handle availability check — handle: '{}', available: {}", handle, available);
        return ResponseEntity.ok(ApiResponse.success(available));
    }

    // =========================================================================
    // Static Metadata Endpoints — Public
    // =========================================================================

    /**
     * Retrieve all valid seller identity types with display metadata.
     *
     * <p>Returns pre-computed static data derived from {@link SellerIdentityType} enum. This
     * endpoint is intentionally public (no authentication required) because it is consumed by the
     * seller registration form before a user has authenticated.
     *
     * <p>Responses carry a {@value STATIC_METADATA_CACHE_TTL_DAYS}-day {@code Cache-Control} header
     * — clients and CDNs should cache this response aggressively.
     *
     * @return list of identity types with name, display label, and description
     */
    @GetMapping("/identity-types")
    @Observed(name = "seller.lookup.identity-types", contextualName = "seller-get-identity-types")
    @Operation(
            summary = "Get available identity types",
            description =
                    """
                    Retrieve the list of valid seller identity types for registration form population.

                    **Available Types:**
                    - `INDIVIDUAL` — Individual sellers (farmers, freelancers, sole traders)
                    - `BUSINESS` — Registered business entities (Pvt Ltd, LLP, Partnership)

                    **Authorization:** None required — public endpoint.

                    **Caching:** Response is cacheable for up to 1 day. Clients should
                    respect the `Cache-Control: public, max-age=86400, immutable` header.
                    """)
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Identity types retrieved successfully",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "503",
                description = "Service Unavailable — Server error during response serialization")
    })
    public ResponseEntity<ApiResponse<List<SellerIdentityTypeResponse>>> getIdentityTypes() {
        return ResponseEntity.ok()
                .cacheControl(STATIC_METADATA_CACHE)
                .body(ApiResponse.success(IDENTITY_TYPES));
    }

    /**
     * Retrieve all valid seller business types with display metadata.
     *
     * <p>Returns pre-computed static data derived from {@link SellerBusinessType} enum. This
     * endpoint is intentionally public (no authentication required) because it is consumed by the
     * seller registration form before a user has authenticated.
     *
     * <p>Responses carry a {@value STATIC_METADATA_CACHE_TTL_DAYS}-day {@code Cache-Control} header
     * — clients and CDNs should cache this response aggressively.
     *
     * @return list of business types with name and display label
     */
    @GetMapping("/business-types")
    @Observed(name = "seller.lookup.business-types", contextualName = "seller-get-business-types")
    @Operation(
            summary = "Get available business types",
            description =
                    """
                    Retrieve the list of valid seller business types for registration form population.

                    **Available Types:** FARMER, RETAILER, WHOLESALER, MANUFACTURER, and others.

                    **Authorization:** None required — public endpoint.

                    **Caching:** Response is cacheable for up to 1 day. Clients should
                    respect the `Cache-Control: public, max-age=86400, immutable` header.
                    """)
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Business types retrieved successfully",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "503",
                description = "Service Unavailable — Server error during response serialization")
    })
    public ResponseEntity<ApiResponse<List<SellerBusinessTypeResponse>>> getBusinessTypes() {
        return ResponseEntity.ok()
                .cacheControl(STATIC_METADATA_CACHE)
                .body(ApiResponse.success(BUSINESS_TYPES));
    }
}
