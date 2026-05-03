package com.eshop.app.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "csrf_tokens", indexes = {
    @Index(name = "idx_csrf_token_token", columnList = "token", unique = true),
    @Index(name = "idx_csrf_token_user", columnList = "user_id"),
    @Index(name = "idx_csrf_token_session", columnList = "session_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CsrfToken extends BaseEntity {
    
    @Column(nullable = false, unique = true, length = 255)
    private String token;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    
    @Column(name = "session_id", length = 255)
    private String sessionId;
    
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    
    @Column(name = "ip_address", length = 50)
    private String ipAddress;
}
