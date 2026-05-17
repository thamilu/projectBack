package com.eshop.app.catalog.application.port.in;

import com.eshop.app.core.api.response.HomeResponse;
import com.eshop.app.user.domain.entity.User;
import org.springframework.security.core.Authentication;

/**
 * Inbound Port for Home/Dashboard Use Cases.
 */
public interface HomeUseCase {
    HomeResponse getHomePageData(Authentication authentication);
    HomeResponse getHomePageDataForUser(User user);
}

