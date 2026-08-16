package com.eshop.app.user.application.service.impl;

import com.eshop.app.core.util.DateTimeUtils;
import com.eshop.app.user.application.port.in.UserAnalyticsUseCase;
import com.eshop.app.user.domain.repository.UserRepository;
import com.eshop.app.user.shared.domain.enums.UserRole;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
@org.springframework.context.annotation.Primary
public class UserAnalyticsService implements UserAnalyticsUseCase {

    private final UserRepository userRepository;

    @Override
    public long getTotalUserCount() {
        return userRepository.count();
    }

    @Override
    public long getCustomerCount() {
        return userRepository.countByRole(UserRole.CUSTOMER);
    }

    @Override
    public long getSellerCount() {
        return userRepository.countByRole(UserRole.SELLER);
    }

    @Override
    public long getDeliveryAgentCount() {
        return userRepository.countByRole(UserRole.DELIVERY_AGENT);
    }

    @Override
    public long getActiveUserCount() {
        return userRepository.count();
    }

    @Override
    public long getNewUsersThisMonth() {
        return userRepository.countByCreatedAtAfter(DateTimeUtils.startOfMonth());
    }

    @Override
    public List<Map<String, Object>> getUserGrowthData() {
        return userRepository.getUserGrowthData();
    }
}
