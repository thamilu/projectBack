package com.eshop.app.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "cookie_consents", indexes = {
    @Index(name = "idx_cookie_consent_user", columnList = "user_id"),
    @Index(name = "idx_cookie_consent_ip", columnList = "ip_address")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CookieConsent extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    
    @Column(name = "ip_address", nullable = false, length = 50)
    private String ipAddress;
    
    @Column(name = "analytics_consent", nullable = false)
    @Builder.Default
    private Boolean analyticsConsent = false;
    
    @Column(name = "marketing_consent", nullable = false)
    @Builder.Default
    private Boolean marketingConsent = false;
    
    @Column(name = "functional_consent", nullable = false)
    @Builder.Default
    private Boolean functionalConsent = true;
    
    @Column(name = "consented_at", nullable = false)
    private LocalDateTime consentedAt;
    
    @Column(name = "user_agent", length = 500)
    private String userAgent;
}
