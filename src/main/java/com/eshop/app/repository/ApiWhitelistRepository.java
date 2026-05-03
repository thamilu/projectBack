package com.eshop.app.repository;

import com.eshop.app.entity.ApiWhitelist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApiWhitelistRepository extends JpaRepository<ApiWhitelist, Long> {
    Optional<ApiWhitelist> findByIpAddressAndActiveTrue(String ipAddress);
    List<ApiWhitelist> findByActiveTrue();
    boolean existsByIpAddressAndActiveTrue(String ipAddress);
}
