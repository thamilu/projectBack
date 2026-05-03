package com.eshop.app.repository;

import com.eshop.app.entity.Brand;
import com.eshop.app.entity.BrandTranslation;
import com.eshop.app.entity.Language;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BrandTranslationRepository extends JpaRepository<BrandTranslation, Long> {
    Optional<BrandTranslation> findByBrandAndLanguage(Brand brand, Language language);
    List<BrandTranslation> findByBrand(Brand brand);
    List<BrandTranslation> findByLanguage(Language language);
}
