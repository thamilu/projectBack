package com.eshop.app.catalog.application.service.impl;

import com.eshop.app.catalog.api.request.BatchProductCreateRequest;
import com.eshop.app.catalog.api.request.ProductCreateRequest;
import com.eshop.app.catalog.api.request.ProductCreateWithCategoryRequest;
import com.eshop.app.catalog.api.response.ProductResponse;
import com.eshop.app.catalog.application.mapper.ProductMapper;
import com.eshop.app.catalog.application.service.AttributeService;
import com.eshop.app.catalog.application.service.AttributeValidatorService;
import com.eshop.app.catalog.application.service.CatalogGovernanceService;
import com.eshop.app.catalog.application.service.ProductServiceHelper;
import com.eshop.app.catalog.domain.entity.*;
import com.eshop.app.catalog.domain.repository.BrandRepository;
import com.eshop.app.catalog.domain.repository.CategoryRepository;
import com.eshop.app.catalog.domain.repository.MasterProductRepository;
import com.eshop.app.catalog.domain.repository.ProductDuplicateCandidateRepository;
import com.eshop.app.catalog.domain.repository.ProductRepository;
import com.eshop.app.core.api.response.BatchOperationResult;
import com.eshop.app.core.events.domain.ProductCreatedEvent;
import com.eshop.app.core.exception.business.DuplicateResourceException;
import com.eshop.app.core.exception.business.ResourceNotFoundException;
import static com.eshop.app.core.infrastructure.config.security.SecurityExpressions.*;
import com.eshop.app.store.application.service.impl.StoreResolver;
import com.eshop.app.store.domain.entity.Store;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class ProductCreateService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final MasterProductRepository masterProductRepository;
    private final ProductDuplicateCandidateRepository candidateRepository;
    private final ProductMapper productMapper;
    private final ProductServiceHelper helper;
    private final CatalogGovernanceService catalogGovernanceService;
    private final ApplicationEventPublisher eventPublisher;
    private final StoreResolver storeResolver;
    private final AttributeValidatorService attributeValidatorService;
    private final AttributeService attributeService;

    @Transactional(rollbackFor = Exception.class)
    @PreAuthorize(IS_ADMIN_OR_SELLER)
    public ProductResponse createProduct(
            @Valid @NotNull ProductCreateRequest request, String userId) {
        MDC.put("operation", "createProduct");
        MDC.put("sku", request.getSku());
        MDC.put("userId", userId);
        log.info(
                "Creating product: SKU={}, name={}, userId={}",
                request.getSku(),
                request.getName(),
                userId);

        try {
            if (request.getCategoryType() != null && request.getAttributes() != null) {
                attributeValidatorService.validateAttributes(
                        request.getCategoryType(), request.getAttributes());
            }

            if (productRepository.existsBySku(request.getSku())) {
                throw new com.eshop.app.catalog.shared.exception.DuplicateSkuException(
                        request.getSku());
            }

            Category category =
                    categoryRepository
                            .findById(request.getCategoryId())
                            .orElseThrow(
                                    () ->
                                            new ResourceNotFoundException(
                                                    "Category not found with id: "
                                                            + request.getCategoryId()));

            Store store;
            try {
                store = storeResolver.resolveStore(request.getStoreId());
            } catch (ResourceNotFoundException e) {
                throw new ResourceNotFoundException(
                        "Seller must create a store before adding products. Please create your"
                                + " store first.");
            }

            Brand brand = null;
            if (request.getBrandId() != null) {
                brand =
                        brandRepository
                                .findById(request.getBrandId())
                                .orElseThrow(
                                        () ->
                                                new ResourceNotFoundException(
                                                        "Brand not found with id: "
                                                                + request.getBrandId()));
            }

            Set<Tag> tags = helper.resolveOrCreateTags(request.getTags());
            MasterProduct duplicateMaster = null;

            if (request.getParentMasterProductId() != null) {
                MasterProduct parentMaster =
                        masterProductRepository
                                .findById(request.getParentMasterProductId())
                                .orElseThrow(
                                        () ->
                                                new ResourceNotFoundException(
                                                        "Parent Master Product not found with id: "
                                                                + request
                                                                        .getParentMasterProductId()));

                MasterProduct rootMaster =
                        parentMaster.getRootMasterProduct() != null
                                ? parentMaster.getRootMasterProduct()
                                : parentMaster;
                String slug = helper.generateOrEnsureUniqueFriendlyUrl(request);
                String sanitizedDesc = helper.sanitize(request.getDescription());
                String shortDesc =
                        sanitizedDesc != null && sanitizedDesc.length() > 200
                                ? sanitizedDesc.substring(0, 200)
                                : sanitizedDesc;

                Brand masterBrand = brand;
                if (brand != null
                        && parentMaster.getBrand() != null
                        && parentMaster.getBrand().getId().equals(brand.getId())) {
                    masterBrand = null;
                }
                Category masterCategory = category;
                if (category != null
                        && parentMaster.getCategory() != null
                        && parentMaster.getCategory().getId().equals(category.getId())) {
                    masterCategory = null;
                }

                MasterProduct newMaster =
                        MasterProduct.builder()
                                .name(request.getName())
                                .slug(slug)
                                .brand(masterBrand)
                                .category(masterCategory)
                                .baseDescription(sanitizedDesc)
                                .shortDescription(shortDesc)
                                .specifications(
                                        request.getAttributes() != null
                                                ? request.getAttributes().toString()
                                                : null)
                                .active(true)
                                .approvalStatus(MasterProductApprovalStatus.PENDING_APPROVAL)
                                .productType(
                                        com.eshop.app.catalog.domain.entity.MasterProductType
                                                .DERIVED)
                                .parentMasterProduct(parentMaster)
                                .rootMasterProduct(rootMaster)
                                .derivedFromSellerId(userId)
                                .derivedReason(request.getDerivedReason())
                                .createdFromCatalog(true)
                                .duplicateStatus("NONE")
                                .media(new ArrayList<>())
                                .build();

                duplicateMaster = masterProductRepository.save(newMaster);
            } else {
                CatalogGovernanceService.CatalogCheckResult checkResult =
                        catalogGovernanceService.checkDuplicates(
                                request.getName(),
                                category.getId(),
                                brand != null ? brand.getId() : null);

                if (checkResult.hasExactMatch()) {
                    duplicateMaster = checkResult.getExactMatch();
                } else {
                    String slug = helper.generateOrEnsureUniqueFriendlyUrl(request);
                    String initialDuplicateStatus =
                            checkResult.hasSimilarMatch() ? "PENDING_DUPLICATE_REVIEW" : "NONE";
                    String sanitizedDesc = helper.sanitize(request.getDescription());
                    String shortDesc =
                            sanitizedDesc != null && sanitizedDesc.length() > 200
                                    ? sanitizedDesc.substring(0, 200)
                                    : sanitizedDesc;

                    MasterProduct newMaster =
                            MasterProduct.builder()
                                    .name(request.getName())
                                    .slug(slug)
                                    .brand(brand)
                                    .category(category)
                                    .baseDescription(sanitizedDesc)
                                    .shortDescription(shortDesc)
                                    .specifications(
                                            request.getAttributes() != null
                                                    ? request.getAttributes().toString()
                                                    : null)
                                    .active(true)
                                    .approvalStatus(MasterProductApprovalStatus.APPROVED)
                                    .duplicateStatus(initialDuplicateStatus)
                                    .media(new ArrayList<>())
                                    .build();

                    duplicateMaster = masterProductRepository.save(newMaster);

                    if (checkResult.hasSimilarMatch()) {
                        ProductDuplicateCandidate candidate =
                                ProductDuplicateCandidate.builder()
                                        .sourceProductId(duplicateMaster.getId())
                                        .matchedProductId(checkResult.getSimilarMatch().getId())
                                        .similarityScore(checkResult.getSimilarityScore())
                                        .reviewStatus("PENDING")
                                        .build();
                        candidateRepository.save(candidate);
                    }
                }
            }

            Product product = helper.buildProductFromRequest(request, category, store, brand, tags);
            product.setMasterProduct(duplicateMaster);
            product = productRepository.save(product);

            eventPublisher.publishEvent(new ProductCreatedEvent(this, product));
            log.info(
                    "Successfully created product: ID={}, SKU={}",
                    product.getId(),
                    product.getSku());
            return productMapper.toProductResponse(product);

        } finally {
            MDC.clear();
        }
    }

    @Transactional
    @PreAuthorize(IS_ADMIN_OR_SELLER)
    public ProductResponse createProductWithAutoCategory(ProductCreateWithCategoryRequest request) {
        Category category;
        if (request.getCategoryId() != null) {
            category =
                    categoryRepository
                            .findById(request.getCategoryId())
                            .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        } else if (request.getNewCategoryName() != null
                && !request.getNewCategoryName().isEmpty()) {
            category =
                    categoryRepository
                            .findByName(request.getNewCategoryName())
                            .orElseGet(
                                    () ->
                                            categoryRepository.save(
                                                    new Category(request.getNewCategoryName())));
        } else {
            throw new IllegalArgumentException(
                    "Either categoryId or newCategoryName must be provided");
        }

        if (request.getAttributes() != null && !request.getAttributes().isEmpty()) {
            attributeService.validateProductAttributes(category.getId(), request.getAttributes());
        }

        Product product =
                Product.builder()
                        .name(request.getName())
                        .price(request.getPrice())
                        .category(category)
                        .status(ProductStatus.ACTIVE)
                        .build();
        product = productRepository.save(product);
        return productMapper.toProductResponse(product);
    }

    @Transactional
    @PreAuthorize(IS_ADMIN_OR_SELLER)
    public ProductResponse cloneProductToSellerStore(Long masterProductId, String userId) {
        log.info("Cloning master product {} for seller {}", masterProductId, userId);
        MasterProduct masterProduct =
                masterProductRepository
                        .findById(masterProductId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Master product not found with id: "
                                                        + masterProductId));

        Store store;
        try {
            store = storeResolver.resolveCurrentSellerStore();
        } catch (ResourceNotFoundException e) {
            throw new ResourceNotFoundException(
                    "Seller must create a store before listing products.");
        }

        String sku = helper.generateUniqueSku(store.getId(), masterProduct.getId());
        String friendlyUrlBase =
                helper.generateFriendlyUrl(masterProduct.getName()) + "-store-" + store.getId();
        String friendlyUrl = helper.ensureUniqueFriendlyUrl(friendlyUrlBase);

        Product product =
                Product.builder()
                        .masterProduct(masterProduct)
                        .store(store)
                        .name(masterProduct.getName())
                        .description(masterProduct.getBaseDescription())
                        .shortDescription(masterProduct.getShortDescription())
                        .sku(sku)
                        .friendlyUrl(friendlyUrl)
                        .price(BigDecimal.ZERO)
                        .status(ProductStatus.DRAFT)
                        .category(masterProduct.getCategory())
                        .brand(masterProduct.getBrand())
                        .build();

        product.setStockQuantity(0);
        product = productRepository.save(product);
        return productMapper.toProductResponse(product);
    }

    @Transactional
    @PreAuthorize(IS_ADMIN)
    public BatchOperationResult<ProductResponse> createProductsBatch(
            BatchProductCreateRequest request) {
        log.info("Starting batch product creation: {} items", request.products().size());
        BatchOperationResult.Builder<ProductResponse> resultBuilder =
                BatchOperationResult.builder();
        String adminUserId = "admin-batch";

        for (int i = 0; i < request.products().size(); i++) {
            ProductCreateRequest req = request.products().get(i);
            try {
                ProductResponse created = createProduct(req, adminUserId);
                resultBuilder.addSuccess(created);
            } catch (DuplicateResourceException e) {
                resultBuilder.addFailure(i, req.getSku(), e.getMessage(), "DUPLICATE_SKU");
            } catch (ResourceNotFoundException e) {
                resultBuilder.addFailure(i, req.getName(), e.getMessage(), "NOT_FOUND");
            } catch (Exception e) {
                resultBuilder.addFailure(i, req.getName(), e.getMessage(), "INTERNAL_ERROR");
            }

            if (resultBuilder.hasFailures() && request.options().stopOnError()) {
                break;
            }
        }
        return resultBuilder.build();
    }
}
