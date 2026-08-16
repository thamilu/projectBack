package com.eshop.app.catalog.application.service.impl;

import com.eshop.app.catalog.api.response.MasterProductResponse;
import com.eshop.app.catalog.application.mapper.MasterProductMapper;
import com.eshop.app.catalog.domain.entity.MasterProduct;
import com.eshop.app.catalog.domain.repository.MasterProductRepository;
import com.eshop.app.core.api.response.PageResponse;
import com.eshop.app.core.exception.business.ResourceNotFoundException;
import static com.eshop.app.core.infrastructure.config.security.SecurityExpressions.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class MasterProductService {

    private final MasterProductRepository masterProductRepository;
    private final MasterProductMapper masterProductMapper;

    @Transactional(readOnly = true)
    @PreAuthorize(IS_ADMIN_OR_SELLER)
    public PageResponse<MasterProductResponse> getMasterProducts(String search, Pageable pageable) {
        log.info("Fetching master products for dashboard browsing. Search: {}", search);
        Page<MasterProduct> page;
        if (search != null && !search.trim().isEmpty()) {
            page = masterProductRepository.searchActive(search.trim(), pageable);
        } else {
            page = masterProductRepository.findAllActive(pageable);
        }
        return PageResponse.of(page, masterProductMapper::toResponse);
    }

    @Transactional(readOnly = true)
    @PreAuthorize(IS_ADMIN_OR_SELLER)
    public MasterProductResponse getMasterProductById(@NotNull @Positive Long id) {
        log.info("Fetching master product by ID: {}", id);
        MasterProduct mp =
                masterProductRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Master product not found with id: " + id));
        return masterProductMapper.toResponse(mp);
    }
}
