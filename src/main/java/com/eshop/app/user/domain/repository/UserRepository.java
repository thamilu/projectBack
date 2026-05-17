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
}
