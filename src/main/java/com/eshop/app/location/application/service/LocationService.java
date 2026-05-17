package com.eshop.app.location.application.service;

import com.eshop.app.location.api.response.CountryDTO;
import com.eshop.app.location.api.response.DistrictDTO;
import com.eshop.app.location.api.response.StateDTO;
import com.eshop.app.location.api.response.TalukDTO;
import com.eshop.app.location.domain.entity.PostalCode;
import com.eshop.app.location.domain.repository.CountryRepository;
import com.eshop.app.location.domain.repository.DistrictRepository;
import com.eshop.app.location.domain.repository.PostalCodeRepository;
import com.eshop.app.location.domain.repository.StateRepository;
import com.eshop.app.location.domain.repository.TalukRepository;
import com.eshop.app.location.api.response.LocalityDTO;
import com.eshop.app.location.api.response.LocationResponseDTO;
import com.eshop.app.location.api.response.PincodeDTO;




import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for location / pincode lookup operations.
 *
 * <p>Design decisions:
 * <ul>
 *   <li>All queries are {@code readOnly} — reduces locking overhead.</li>
 *   <li>DTO mapping is done in-memory from eagerly-fetched entities —
 *       no additional queries after the repository call.</li>
 *   <li>Prepared for Redis {@code @Cacheable} integration on {@code getByPinCode}.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocationService {

    private final PostalCodeRepository postalCodeRepository;
    private final StateRepository stateRepository;
    private final DistrictRepository districtRepository;
    private final TalukRepository talukRepository;
    private final CountryRepository countryRepository;

    /**
     * Fetch all active states.
     */
    @Transactional(readOnly = true)
    public List<StateDTO> getAllStates() {
        return stateRepository.findAll().stream()
                .filter(s -> s.getIsActive())
                .map(s -> new StateDTO(s.getId(), s.getName(), s.getStateCode()))
                .toList();
    }

    /**
     * Fetch all countries.
     */
    @Transactional(readOnly = true)
    public List<CountryDTO> getAllCountries() {
        return countryRepository.findAll().stream()
                .map(c -> new CountryDTO(c.getId(), c.getName(), c.getIsoCode()))
                .toList();
    }

    /**
     * Fetch districts for a state.
     */
    @Transactional(readOnly = true)
    public List<DistrictDTO> getDistrictsByState(Long stateId) {
        return districtRepository.findByState_Id(stateId).stream()
                .map(d -> new DistrictDTO(d.getId(), d.getName(), stateId))
                .toList();
    }

    /**
     * Fetch taluks for a district.
     */
    @Transactional(readOnly = true)
    public List<TalukDTO> getTaluksByDistrict(Long districtId) {
        return talukRepository.findByDistrict_Id(districtId).stream()
                .map(t -> new TalukDTO(t.getId(), t.getName(), districtId))
                .toList();
    }

    /**
     * Fetch pincodes for a district (legacy support).
     */
    @Transactional(readOnly = true)
    public List<PincodeDTO> getPincodesByDistrict(Long districtId) {
        return postalCodeRepository.findByDistrict_Id(districtId).stream()
                .map(pc -> new PincodeDTO(pc.getId(), pc.getPinCode(), districtId, 
                    pc.getTaluk() != null ? pc.getTaluk().getName() : null, pc.getPostOfficeName()))
                .toList();
    }

    /**
     * Fetch pincodes for a taluk.
     */
    @Transactional(readOnly = true)
    public List<PincodeDTO> getPincodesByTaluk(Long talukId) {
        return postalCodeRepository.findByTaluk_Id(talukId).stream()
                .map(pc -> new PincodeDTO(pc.getId(), pc.getPinCode(), pc.getDistrict().getId(), 
                    pc.getTaluk().getName(), pc.getPostOfficeName()))
                .toList();
    }

    /**
     * Looks up all active localities for the given pincode.
     *
     * @param pinCode the pincode to search (e.g. "570008")
     * @return response DTO; {@code localities} is empty if pincode is unknown
     */
    @Transactional(readOnly = true)
    public LocationResponseDTO getByPinCode(String pinCode) {
        log.debug("Location lookup for pincode: {}", pinCode);

        List<PostalCode> results = postalCodeRepository.findActiveByPinCode(pinCode);

        if (results.isEmpty()) {
            log.debug("No postal data found for pincode: {}", pinCode);
            return new LocationResponseDTO(pinCode, null, null, null, null, null, null, List.of());
        }

        // All rows for the same pincode share the same state/district/country
        PostalCode first = results.get(0);

        List<LocalityDTO> localities = results.stream()
                .map(pc -> new LocalityDTO(pc.getLocalityName(), pc.getPostOfficeName()))
                .toList();

        return new LocationResponseDTO(
                pinCode,
                first.getCountry().getName(),
                first.getCountry().getIsoCode(),
                first.getState().getName(),
                first.getState().getStateCode(),
                first.getDistrict().getName(),
                first.getTaluk() != null ? first.getTaluk().getName() : null,
                localities
        );
    }

    /**
     * Search unique pincodes by prefix (for frontend autocomplete).
     */
    @Transactional(readOnly = true)
    public List<String> searchPinCodes(String prefix) {
        if (prefix == null || prefix.length() < 1) {
            return List.of();
        }
        return postalCodeRepository.findUniquePinCodesByPrefix(prefix);
    }
}
