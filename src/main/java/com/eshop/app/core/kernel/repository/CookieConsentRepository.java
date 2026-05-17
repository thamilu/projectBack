package com.eshop.app.core.kernel.repository;

import com.eshop.app.core.kernel.entity.CookieConsent;
import com.eshop.app.user.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CookieConsentRepository extends JpaRepository<CookieConsent, Long> {
    List<CookieConsent> findByUser(User user);
    Optional<CookieConsent> findByIpAddress(String ipAddress);
    List<CookieConsent> findByAnalyticsConsentTrue();
    List<CookieConsent> findByMarketingConsentTrue();
}
