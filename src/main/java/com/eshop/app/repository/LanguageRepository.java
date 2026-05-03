package com.eshop.app.repository;

import com.eshop.app.entity.Language;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LanguageRepository extends JpaRepository<Language, Long> {
    Optional<Language> findByCode(String code);
    Optional<Language> findByIsDefaultTrue();
    List<Language> findByActiveTrueOrderBySortOrderAsc();
    boolean existsByCode(String code);
}
