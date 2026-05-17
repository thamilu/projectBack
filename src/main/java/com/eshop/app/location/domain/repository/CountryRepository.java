package com.eshop.app.location.domain.repository;

import com.eshop.app.location.domain.entity.Country;




import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for {@link Country} master data.
 * Read-heavy — used primarily by the seeder and admin APIs.
 */
@Repository
public interface CountryRepository extends JpaRepository<Country, Long> {

    Optional<Country> findByIsoCodeIgnoreCase(String isoCode);

    Optional<Country> findBySlug(String slug);

    boolean existsByIsoCodeIgnoreCase(String isoCode);
}
