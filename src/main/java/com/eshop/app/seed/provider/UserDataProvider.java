package com.eshop.app.seed.provider;

import com.eshop.app.core.infrastructure.config.properties.SeedProperties;
import java.util.List;

/**
 * Interface for providing user seed data.
 */
public interface UserDataProvider {
    List<SeedProperties.UserSeed> getUsers();
}
