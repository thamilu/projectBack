package com.eshop.app.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "api_whitelists", indexes = {
    @Index(name = "idx_api_whitelist_ip", columnList = "ip_address"),
    @Index(name = "idx_api_whitelist_active", columnList = "active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiWhitelist extends BaseEntity {
    
    @Column(name = "ip_address", nullable = false, length = 50)
    private String ipAddress;
    
    @Column(length = 500)
    private String description;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    @Column(name = "created_by_user_id")
    private Long createdByUserId;
}
