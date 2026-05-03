package com.eshop.app.repository;

import com.eshop.app.entity.Sitemap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SitemapRepository extends JpaRepository<Sitemap, Long> {
    List<Sitemap> findByActiveTrue();
    Optional<Sitemap> findByEntityTypeAndEntityId(String entityType, Long entityId);
    List<Sitemap> findByEntityType(String entityType);
}
