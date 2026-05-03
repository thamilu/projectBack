package com.eshop.app.repository;

import com.eshop.app.entity.SellerProfile;

import com.eshop.app.enums.SellerIdentityType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SellerProfileRepository extends JpaRepository<SellerProfile, Long> {

    Optional<SellerProfile> findByUser_Id(Long userId);

    boolean existsByUser_Id(Long userId);

    @Query("SELECT COUNT(sp) FROM SellerProfile sp WHERE sp.identityType = :type")
    long countByIdentityType(@Param("type") SellerIdentityType type);

    @Query("SELECT sp FROM SellerProfile sp WHERE sp.user.keycloakId = :keycloakId")
    Optional<SellerProfile> findByUser_KeycloakId(@Param("keycloakId") String keycloakId);

    /**
     * Fetch seller profile together with User and UserProfile in a single SQL join.
     * Use this for any endpoint that needs to return personal info (name, phone, etc.)
     * alongside seller business info — avoids the N+1 problem.
     *
     * Architecture: seller_profiles.user_id → users.id → user_profiles.user_id
     * No direct FK from seller_profiles to user_profiles (industry standard).
     */
    @EntityGraph(value = "SellerProfile.full")
    @Query("SELECT sp FROM SellerProfile sp " +
           "LEFT JOIN FETCH sp.user u " +
           "LEFT JOIN FETCH u.userProfile " +
           "WHERE u.id = :userId")
    Optional<SellerProfile> findByUserIdWithProfile(@Param("userId") Long userId);

    @EntityGraph(value = "SellerProfile.full")
    @Query("SELECT sp FROM SellerProfile sp " +
           "LEFT JOIN FETCH sp.user u " +
           "LEFT JOIN FETCH u.userProfile " +
           "WHERE u.keycloakId = :keycloakId")
    Optional<SellerProfile> findByKeycloakIdWithProfile(@Param("keycloakId") String keycloakId);

    @EntityGraph(value = "SellerProfile.full")
    @Query("SELECT sp FROM SellerProfile sp WHERE sp.status = 'PENDING'")
    java.util.List<SellerProfile> findAllPendingWithDetails();
}
