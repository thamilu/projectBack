package com.eshop.app.repository;

import com.eshop.app.entity.Taluk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for {@link Taluk} master data.
 */
@Repository
public interface TalukRepository extends JpaRepository<Taluk, Long> {

    java.util.List<Taluk> findByDistrict_Id(Long districtId);

    Optional<Taluk> findByDistrict_IdAndNameIgnoreCase(Long districtId, String name);

    Optional<Taluk> findBySlug(String slug);

    Optional<Taluk> findByDistrict_IdAndSlug(Long districtId, String slug);
}
