package com.eshop.app.catalog.domain.repository;

import com.eshop.app.catalog.domain.entity.Product;
import com.eshop.app.catalog.domain.entity.ProductTranslation;
import com.eshop.app.localization.domain.entity.Language;




import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductTranslationRepository extends JpaRepository<ProductTranslation, Long> {
    Optional<ProductTranslation> findByProductAndLanguage(Product product, Language language);
    List<ProductTranslation> findByProduct(Product product);
    List<ProductTranslation> findByLanguage(Language language);
    Optional<ProductTranslation> findByProductIdAndLanguageId(Long productId, Long languageId);
}
