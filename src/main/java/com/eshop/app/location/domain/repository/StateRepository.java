package com.eshop.app.location.domain.repository;

import com.eshop.app.location.domain.entity.State;




import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for {@link State} master data.
 */
@Repository
public interface StateRepository extends JpaRepository<State, Long> {

    Optional<State> findByCountry_IdAndNameIgnoreCase(Long countryId, String name);

    Optional<State> findBySlug(String slug);

    Optional<State> findByCountry_IdAndSlug(Long countryId, String slug);

    boolean existsByCountry_IdAndStateCodeIgnoreCase(Long countryId, String stateCode);
}
