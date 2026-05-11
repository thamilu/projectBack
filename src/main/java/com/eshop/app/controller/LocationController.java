package com.eshop.app.controller;

import com.eshop.app.constants.ApiConstants;
import com.eshop.app.dto.location.*;
import com.eshop.app.dto.response.ApiResponse;
import com.eshop.app.service.LocationService;
import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for location / pincode lookup.
 *
 * <p>Follows the pincode-first eCommerce UX pattern:
 * <ol>
 *   <li>User enters pincode.</li>
 *   <li>Frontend calls {@code GET /api/v1/locations/pincode/{pincode}}.</li>
 *   <li>State, district, and localities are auto-filled from the response.</li>
 * </ol>
 *
 * <p>This endpoint is intentionally <b>public</b> — no auth required.
 * Rate-limiting is handled at the gateway layer.
 */
@Tag(name = "Locations", description = "Pincode-first address lookup — powers the eCommerce address auto-fill flow")
@RestController
@RequestMapping(ApiConstants.Endpoints.LOCATIONS)
@Validated
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    /**
     * Look up location data for the given pincode.
     *
     * @param pincode Indian pincode (6 digits) or international postal code (3–10 chars).
     * @return full location hierarchy with a list of localities for the frontend dropdown.
     */
    @GetMapping("/pincode/{pincode}")
    @Operation(
        summary = "Pincode lookup",
        description = "Returns state, district, and locality list for a given pincode. "
                + "Designed for the eCommerce address auto-fill flow (Amazon/Flipkart style)."
    )
    public ResponseEntity<ApiResponse<LocationResponseDTO>> getByPinCode(
            @Parameter(description = "Pincode — 3 to 10 characters (e.g. 570008)")
            @PathVariable
            @Size(min = 3, max = 10, message = "Pincode must be between 3 and 10 characters")
            String pincode) {
 
     LocationResponseDTO response = locationService.getByPinCode(pincode);
         return ResponseEntity.ok(ApiResponse.success(response));
     }
 
     @GetMapping("/pincode/search")
     @Operation(summary = "Search unique pincodes by prefix")
     public ResponseEntity<ApiResponse<List<String>>> searchPinCodes(@RequestParam String query) {
         return ResponseEntity.ok(ApiResponse.success(locationService.searchPinCodes(query)));
     }

    @GetMapping("/countries")
    @Operation(summary = "List all countries")
    public ResponseEntity<ApiResponse<List<CountryDTO>>> getCountries() {
        return ResponseEntity.ok(ApiResponse.success(locationService.getAllCountries()));
    }

    @GetMapping("/states")
    @Operation(summary = "List all states")
    public ResponseEntity<ApiResponse<List<StateDTO>>> getStates() {
        return ResponseEntity.ok(ApiResponse.success(locationService.getAllStates()));
    }

    @GetMapping("/districts")
    @Operation(summary = "List districts for a state")
    public ResponseEntity<ApiResponse<List<DistrictDTO>>> getDistricts(@RequestParam Long stateId) {
        return ResponseEntity.ok(ApiResponse.success(locationService.getDistrictsByState(stateId)));
    }

    @GetMapping("/taluks")
    @Operation(summary = "List taluks for a district")
    public ResponseEntity<ApiResponse<List<TalukDTO>>> getTaluks(@RequestParam Long districtId) {
        return ResponseEntity.ok(ApiResponse.success(locationService.getTaluksByDistrict(districtId)));
    }

    @GetMapping("/pincodes")
    @Operation(summary = "List pincodes for a district or taluk")
    public ResponseEntity<ApiResponse<List<PincodeDTO>>> getPincodes(
            @RequestParam(required = false) Long districtId,
            @RequestParam(required = false) Long talukId) {
        if (talukId != null) {
            return ResponseEntity.ok(ApiResponse.success(locationService.getPincodesByTaluk(talukId)));
        }
        return ResponseEntity.ok(ApiResponse.success(locationService.getPincodesByDistrict(districtId)));
    }
}
