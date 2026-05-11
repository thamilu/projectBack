package com.eshop.app.repository;

import com.eshop.app.entity.PostalCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for {@link PostalCode} master data.
 *
 * <p>Query design:
 * <ul>
 *   <li>{@code JOIN FETCH} on all denormalized FKs ensures a single SQL query
 *       for the full location hierarchy — no N+1 issues during DTO mapping.</li>
 *   <li>{@code DISTINCT} eliminates any accidental cartesian-product duplicates.</li>
 * </ul>
 */
@Repository
public interface PostalCodeRepository extends JpaRepository<PostalCode, Long> {

    /**
     * Fetches all active postal code rows for the given pincode, including the full
     * location hierarchy, in a single optimized query.
     *
     * @param pinCode the pincode to look up (e.g. "570008")
     * @return list of matching postal codes with hierarchy eagerly loaded
     */
    @Query("""
            SELECT DISTINCT p
            FROM PostalCode p
            JOIN FETCH p.country
            JOIN FETCH p.state
            JOIN FETCH p.district
            LEFT JOIN FETCH p.taluk
            WHERE p.pinCode = :pinCode
              AND p.isActive = true
            ORDER BY p.localityName
            """)
    List<PostalCode> findActiveByPinCode(@Param("pinCode") String pinCode);
    
    /**
     * Search unique pincodes by prefix for autocomplete.
     * Limits results to the top 10 for performance.
     */
    @Query(value = """
            SELECT DISTINCT pin_code
            FROM postal_codes
            WHERE pin_code LIKE :prefix%
              AND is_active = true
            ORDER BY pin_code
            LIMIT 10
            """, nativeQuery = true)
    List<String> findUniquePinCodesByPrefix(@Param("prefix") String prefix);

    List<PostalCode> findByDistrict_Id(Long districtId);

    List<PostalCode> findByTaluk_Id(Long talukId);

    boolean existsByPinCodeAndLocalityNameIgnoreCaseAndPostOfficeNameIgnoreCase(
            String pinCode, String localityName, String postOfficeName);

    /**
     * Optimized projection to fetch only the keys needed for in-memory deduplication.
     * Prevents loading full entities into the persistence context.
     */
    @Query("SELECT p.pinCode, p.localityName, p.postOfficeName FROM PostalCode p")
    List<Object[]> findAllKeys();
}
