package com.eshop.app.repository;

import com.eshop.app.entity.CookieConsent;
import com.eshop.app.entity.User;
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
