package com.eshop.app.repository;

import com.eshop.app.entity.Category;
import com.eshop.app.entity.CategoryTranslation;
import com.eshop.app.entity.Language;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryTranslationRepository extends JpaRepository<CategoryTranslation, Long> {
    Optional<CategoryTranslation> findByCategoryAndLanguage(Category category, Language language);
    List<CategoryTranslation> findByCategory(Category category);
    List<CategoryTranslation> findByLanguage(Language language);
}
