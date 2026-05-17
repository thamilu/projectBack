package com.eshop.app.location.api.response;

import java.util.List;

/**
 * API response DTO for the pincode lookup endpoint.
 *
 * <p>
 * Response contract for {@code GET /api/v1/locations/pincode/{pincode}}.
 *
 * <p>
 * Example:
 * 
 * <pre>
 * {
 *   "pincode": "570008",
 *   "country": "India",
 *   "countryCode": "IN",
 *   "state": "Karnataka",
 *   "stateCode": "KA",
 *   "district": "Mysuru",
 *   "localities": [
 *     { "locality": "Mysore", "postOffice": "Mysuru South S.O" }
 *   ]
 * }
 * </pre>
 *
 * @param pincode     The searched pincode.
 * @param country     Country name.
 * @param countryCode ISO 3166-1 alpha-2 code (e.g. "IN").
 * @param state       State name.
 * @param stateCode   State abbreviation (e.g. "KA").
 * @param district    District name.
 * @param localities  List of localities / post offices for this pincode.
 */
public record LocationResponseDTO(
                String pincode,
                String country,
                String countryCode,
                String state,
                String stateCode,
                String district,
                String taluk,
                List<LocalityDTO> localities) {
}
