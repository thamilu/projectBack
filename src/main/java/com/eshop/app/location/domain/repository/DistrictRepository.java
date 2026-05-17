package com.eshop.app.location.domain.repository;

import com.eshop.app.location.domain.entity.District;




import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for {@link District} master data.
 */
@Repository
public interface DistrictRepository extends JpaRepository<District, Long> {

    java.util.List<District> findByState_Id(Long stateId);

    Optional<District> findByState_IdAndNameIgnoreCase(Long stateId, String name);

    Optional<District> findBySlug(String slug);

    Optional<District> findByState_IdAndSlug(Long stateId, String slug);
}
