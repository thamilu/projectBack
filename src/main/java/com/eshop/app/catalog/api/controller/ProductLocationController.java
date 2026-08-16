package com.eshop.app.catalog.api.controller;

import com.eshop.app.catalog.api.request.ProductLocationSearchRequest;
import com.eshop.app.catalog.api.response.ProductLocationResponse;
import com.eshop.app.catalog.application.port.in.ProductLocationUseCase;

import com.eshop.app.core.kernel.ApiConstants;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import static com.eshop.app.core.infrastructure.config.security.SecurityExpressions.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiConstants.Endpoints.PRODUCT_LOCATION)
@RequiredArgsConstructor
@Slf4j
@io.swagger.v3.oas.annotations.tags.Tag(name = "Product Location Search", description = "Location-based product search with Google Maps integration")
public class ProductLocationController {

    private final ProductLocationUseCase productLocationService;

    @PostMapping("/search")
    @Operation(summary = "Search products by location", description = """
            Search products based on user's geographic location with advanced filters.

            **Features:**
            - 📍 Search within radius (default 10km, max 500km)
            - 🏪 Filter by specific stores or find nearest stores
            - 💰 Filter by price range
            - 🏷️ Filter by category and brand
            - 🌎 Filter by city, state, or country
            - 🔍 Keyword search across product name, description, SKU
            - 📦 Filter in-stock products only
            - 📊 Sort by: distance, price, rating, newest

            **Distance Calculation:**
            Uses Haversine formula for accurate geographical distance calculation.
            Returns distance in both kilometers and miles.

            **Example Use Cases:**
            1. "Find laptops within 5km of my location"
            2. "Show all electronics in San Francisco sorted by price"
            3. "Find nearest stores selling shoes under $100"
            4. "Products available in California with rating > 4"

            **Google Maps Integration:**
            - Use Google Maps Geolocation API to get user's current location
            - Use Google Places API to geocode store addresses
            - Frontend can display results on Google Maps with markers
            """, security = @SecurityRequirement(name = "bearer-jwt"))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Products found successfully with distance information", content = @Content(schema = @Schema(implementation = Page.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid search parameters (e.g., radius > 500km)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Bearer token required")
    })
    @PreAuthorize(IS_ADMIN_OR_SELLER_OR_CUSTOMER)
    public ResponseEntity<Page<ProductLocationResponse>> searchByLocation(
            @Valid @RequestBody ProductLocationSearchRequest request) {

        log.info("Received location-based search request: lat={}, lng={}, radius={}km",
                request.getLatitude(), request.getLongitude(), request.getRadiusKm());

        Page<ProductLocationResponse> results = productLocationService.searchProductsByLocation(request);

        return ResponseEntity.ok(results);
    }

    @GetMapping("/search")
    @Operation(summary = "Search products by location (GET version)", description = """
            Simplified GET endpoint for location-based product search.
            Use POST endpoint for advanced filters and better performance.

            **Required Parameters:**
            - latitude: User's current latitude
            - longitude: User's current longitude

            **Optional Parameters:**
            - radiusKm: Search radius (default: 10km)
            - keyword: Search term for products
            - city, state, country: Location filters
            - minPrice, maxPrice: Price range
            - categoryId, brandId, storeId: Entity filters
            - inStockOnly: Show only available products (default: false)
            - sortBy: distance, price_asc, price_desc, rating, newest
            - page, size: Pagination parameters
            """, security = @SecurityRequirement(name = "bearer-jwt"))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Products found successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Missing required parameters (latitude, longitude)")
    })
    @PreAuthorize(IS_ADMIN_OR_SELLER_OR_CUSTOMER)
    public ResponseEntity<Page<ProductLocationResponse>> searchByLocationGet(
            @Parameter(description = "User's latitude", required = true, example = "37.7749") @RequestParam Double latitude,

            @Parameter(description = "User's longitude", required = true, example = "-122.4194") @RequestParam Double longitude,

            @Parameter(description = "Search radius in kilometers", example = "10.0") @RequestParam(defaultValue = "10.0") Double radiusKm,

            @Parameter(description = "Search keyword", example = "laptop") @RequestParam(required = false) String keyword,

            @Parameter(description = "Category ID", example = "1") @RequestParam(required = false) Long categoryId,

            @Parameter(description = "Brand ID", example = "1") @RequestParam(required = false) Long brandId,

            @Parameter(description = "Minimum price", example = "100.00") @RequestParam(required = false) String minPrice,

            @Parameter(description = "Maximum price", example = "5000.00") @RequestParam(required = false) String maxPrice,

            @Parameter(description = "City filter", example = "San Francisco") @RequestParam(required = false) String city,

            @Parameter(description = "State filter", example = "California") @RequestParam(required = false) String state,

            @Parameter(description = "Country filter", example = "USA") @RequestParam(required = false) String country,

            @Parameter(description = "Store ID", example = "1") @RequestParam(required = false) Long storeId,

            @Parameter(description = "Show only in-stock products", example = "true") @RequestParam(defaultValue = "false") Boolean inStockOnly,

            @Parameter(description = "Sort by: distance, price_asc, price_desc, rating, newest", example = "distance") @RequestParam(defaultValue = "distance") String sortBy,

            @Parameter(description = "Page number (0-indexed)", example = "0") @RequestParam(defaultValue = "0") Integer page,

            @Parameter(description = "Page size", example = "20") @RequestParam(defaultValue = "20") Integer size) {

        ProductLocationSearchRequest request = ProductLocationSearchRequest.builder()
                .latitude(latitude)
                .longitude(longitude)
                .radiusKm(radiusKm)
                .keyword(keyword)
                .categoryId(categoryId)
                .brandId(brandId)
                .city(city)
                .state(state)
                .country(country)
                .storeId(storeId)
                .inStockOnly(inStockOnly)
                .sortBy(sortBy)
                .page(page)
                .size(size)
                .build();

        Page<ProductLocationResponse> results = productLocationService.searchProductsByLocation(request);

        return ResponseEntity.ok(results);
    }

    @GetMapping("/distance")
    @Operation(summary = "Calculate distance between two points", description = """
            Calculate geographical distance between two points using Haversine formula.
            Returns distance in both kilometers and miles.

            **Use Cases:**
            - Calculate distance from user to store
            - Estimate delivery distance
            - Sort stores by proximity
            """)
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Distance calculated successfully")
    public ResponseEntity<?> calculateDistance(
            @Parameter(description = "Starting latitude", required = true, example = "37.7749") @RequestParam Double lat1,

            @Parameter(description = "Starting longitude", required = true, example = "-122.4194") @RequestParam Double lon1,

            @Parameter(description = "Destination latitude", required = true, example = "37.8044") @RequestParam Double lat2,

            @Parameter(description = "Destination longitude", required = true, example = "-122.2712") @RequestParam Double lon2) {

        Double distanceKm = productLocationService.calculateDistance(lat1, lon1, lat2, lon2);
        Double distanceMiles = productLocationService.convertKmToMiles(distanceKm);

        return ResponseEntity.ok(new DistanceResponse(distanceKm, distanceMiles));
    }

    @GetMapping("/nearby-stores")
    @Operation(summary = "Find nearby stores", description = """
            Find all stores within specified radius from user's location.
            Returns stores sorted by distance.

            **Frontend Integration:**
            - Display stores on Google Maps with markers
            - Show distance to each store
            - Allow users to filter products by selected store
            """, security = @SecurityRequirement(name = "bearer-jwt"))
    @PreAuthorize(IS_ADMIN_OR_SELLER_OR_CUSTOMER)
    public ResponseEntity<Page<ProductLocationResponse>> findNearbyStores(
            @Parameter(description = "User's latitude", required = true) @RequestParam Double latitude,

            @Parameter(description = "User's longitude", required = true) @RequestParam Double longitude,

            @Parameter(description = "Search radius in kilometers", example = "20.0") @RequestParam(defaultValue = "20.0") Double radiusKm,

            @Parameter(description = "Page number", example = "0") @RequestParam(defaultValue = "0") Integer page,

            @Parameter(description = "Page size", example = "50") @RequestParam(defaultValue = "50") Integer size) {

        ProductLocationSearchRequest request = ProductLocationSearchRequest.builder()
                .latitude(latitude)
                .longitude(longitude)
                .radiusKm(radiusKm)
                .sortBy("distance")
                .page(page)
                .size(size)
                .build();

        Page<ProductLocationResponse> results = productLocationService.searchProductsByLocation(request);

        return ResponseEntity.ok(results);
    }

    // Helper DTO for distance response
    public record DistanceResponse(
            @Schema(description = "Distance in kilometers", example = "5.23") Double kilometers,

            @Schema(description = "Distance in miles", example = "3.25") Double miles) {
    }
}
