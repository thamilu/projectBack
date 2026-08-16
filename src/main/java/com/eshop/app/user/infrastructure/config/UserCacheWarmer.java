package com.eshop.app.user.infrastructure.config;

import com.eshop.app.core.infrastructure.config.cache.CacheConfig;
import com.eshop.app.user.application.mapper.UserMapper;
import com.eshop.app.user.domain.entity.User;
import com.eshop.app.user.domain.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.CacheManager;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pre-warms the L2 cache for hot active users on startup to prevent initial performance degradation
 * or database load spikes.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserCacheWarmer {

    private static final int CACHE_WARM_USER_COUNT = 50;

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final CacheManager cacheManager;

    /**
     * Listens to ApplicationReadyEvent and warms the users cache with the top 50 recently active,
     * non-deleted users.
     *
     * <p>Runs within a read-only transaction so the persistence context remains open for entity
     * mapping (avoiding {@code LazyInitializationException} outside of a request context) and so
     * Hibernate can apply read-only optimizations (no dirty checking).
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional(readOnly = true)
    public void warmUserCache() {
        log.info("Starting User Cache Warming...");
        try {
            org.springframework.cache.Cache cache = cacheManager.getCache(CacheConfig.USERS_CACHE);
            if (cache == null) {
                log.warn("Users cache not found in CacheManager");
                return;
            }

            // Warm cache with recently updated, non-deleted active users (top N).
            // Filtering is performed at the database level to guarantee the fetched
            // set actually contains only active users.
            List<User> hotUsers =
                    userRepository
                            .findAllByDeleted(
                                    false,
                                    PageRequest.of(
                                            0,
                                            CACHE_WARM_USER_COUNT,
                                            Sort.by(Sort.Direction.DESC, "updatedAt", "id")))
                            .getContent();

            for (User user : hotUsers) {
                cache.put(user.getId(), userMapper.toUserResponse(user));
            }
            log.info("User cache warmed with {} hot active users", hotUsers.size());
        } catch (Exception e) {
            log.error("Failed to warm user cache", e);
        }
    }
}

