package com.eshop.app.service;

import com.eshop.app.enums.UserAction;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserAuditService {
    public void logUserAction(Long userId, Long targetUserId, UserAction action) {
        // Minimal implementation: real implementation should persist audit log
    }

    public void logBulkAction(Long userId, List<Long> targetIds, UserAction action) {
        // Minimal implementation
    }
}
