package com.eshop.app.user.domain.repository;

import com.eshop.app.user.domain.entity.User;
import com.eshop.app.user.shared.domain.enums.UserRole;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByKeycloakId(String keycloakId);

    java.util.List<User> findByEmail(String email);

    Page<User> findByRole(UserRole role, Pageable pageable);

    java.util.List<User> findAllByRole(UserRole role);

    @Query("SELECT u FROM User u JOIN u.userProfile up WHERE " +
            "LOWER(up.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(up.lastName) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<User> searchUsers(@Param("keyword") String keyword, Pageable pageable);

    // Dashboard Analytics Methods
    long countByRole(UserRole role);

    long countByCreatedAtAfter(java.time.LocalDateTime createdAt);

    @Query("SELECT DATE(u.createdAt) as date, COUNT(u) as count FROM User u GROUP BY DATE(u.createdAt) ORDER BY date DESC")
    java.util.List<java.util.Map<String, Object>> getUserGrowthData();

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"userProfile", "sellerProfile", "deliveryAgentProfile"})
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdWithDetails(@Param("id") Long id);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"userProfile", "sellerProfile", "deliveryAgentProfile"})
    @Query("SELECT u FROM User u WHERE u.id IN :ids")
    java.util.List<User> findAllByIdWithDetails(@Param("ids") java.util.List<Long> ids);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.keycloakId = :keycloakId")
    Optional<User> findByKeycloakIdForUpdate(@Param("keycloakId") String keycloakId);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.email = :email")
    java.util.List<User> findByEmailForUpdate(@Param("email") String email);

    @Query("SELECT u FROM User u WHERE u.role = :role")
    java.util.stream.Stream<User> streamByRole(@Param("role") UserRole role);

    @Query("SELECT u FROM User u")
    java.util.stream.Stream<User> streamAll();

    Page<User> findAllByDeleted(boolean deleted, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.role = :role AND u.deleted = :deleted")
    Page<User> findByRoleAndDeleted(@Param("role") UserRole role, @Param("deleted") boolean deleted, Pageable pageable);

    @Query("SELECT u.updatedAt FROM User u WHERE u.id = :id")
    java.util.Optional<java.time.LocalDateTime> findUpdatedAtById(@Param("id") Long id);
}
