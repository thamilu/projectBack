package com.eshop.app.repository;

import com.eshop.app.entity.Language;
import com.eshop.app.entity.SeoUrl;
import com.eshop.app.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeoUrlRepository extends JpaRepository<SeoUrl, Long> {
    Optional<SeoUrl> findByKeyword(String keyword);
    List<SeoUrl> findByEntityTypeAndEntityId(String entityType, Long entityId);
    Optional<SeoUrl> findByEntityTypeAndEntityIdAndLanguage(String entityType, Long entityId, Language language);
    Optional<SeoUrl> findByEntityTypeAndEntityIdAndLanguageAndStore(String entityType, Long entityId, Language language, Store store);
    List<SeoUrl> findByStoreAndLanguage(Store store, Language language);
}
