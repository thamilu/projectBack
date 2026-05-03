package com.eshop.app.repository;

import com.eshop.app.entity.Store;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoreRepository extends JpaRepository<Store, Long> {

    Optional<Store> findByStoreName(String storeName);

    Optional<Store> findBySellerProfile_UserId(Long userId);

    List<Store> findAllByEmail(String email);

    @Query("SELECT s FROM Store s WHERE s.sellerProfile.user.keycloakId = :keycloakId")
    Optional<Store> findBySellerKeycloakId(@Param("keycloakId") String keycloakId);

    Optional<Store> findByDomain(String domain);

    List<Store> findByActiveTrue();

    boolean existsByDomain(String domain);
    
    boolean existsByStoreName(String storeName);

    Page<Store> findByActive(Boolean active, Pageable pageable);

    @Query("SELECT s FROM Store s WHERE LOWER(s.storeName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(s.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Store> searchStores(@Param("keyword") String keyword, Pageable pageable);

    /**
     * Gets aggregated store statistics.
     * 
     * @return map containing store counts
     */
    @Query("""
        SELECT new map(
            COUNT(s.id) as totalShops,
            COUNT(CASE WHEN s.active = true THEN 1 END) as activeShops
        )
        FROM Store s
        WHERE s.deleted = false
        """)
    java.util.Map<String, Object> getStoreStatistics();
}
