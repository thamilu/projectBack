package com.eshop.app.service.impl;

import com.eshop.app.config.ProductProperties;
import com.eshop.app.dto.request.ProductCreateRequest;
import com.eshop.app.entity.Brand;
import com.eshop.app.entity.Category;
import com.eshop.app.entity.Product;
import com.eshop.app.entity.Store;
import com.eshop.app.entity.Tag;
import com.eshop.app.entity.enums.ProductStatus;
import com.eshop.app.exception.FriendlyUrlGenerationException;
import com.eshop.app.repository.ProductRepository;
import com.eshop.app.repository.TagRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Helper component for Product Service operations.
 * Extracted for clarity and testability.
 * 
 * @since 2.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductServiceHelper {
    
    private final ProductRepository productRepository;
    private final TagRepository tagRepository;
    private final ObjectMapper objectMapper;
    private final ProductProperties productProperties;
    
    /**
     * Build Product entity from creation request (CRITICAL-003 FIX).
     * Single responsibility: entity construction.
     */
    public Product buildProductFromRequest(
            ProductCreateRequest request,
            Category category,
            Store store,
            Brand brand,
            Set<Tag> tags) {
        
        String friendlyUrl = generateOrEnsureUniqueFriendlyUrl(request);
        
        Product.ProductBuilder builder = Product.builder()
            .name(request.getName())
                .description(sanitize(request.getDescription()))
            .sku(request.getSku())
            .friendlyUrl(friendlyUrl)
            .price(request.getPrice())
            .discountPrice(request.getDiscountPrice())
            .stockQuantity(request.getStockQuantity())
            .category(category)
            .store(store)
            .featured(Boolean.TRUE.equals(request.getFeatured()))
            .status(ProductStatus.ACTIVE);
        
        if (brand != null) {
            builder.brand(brand);
        }
        
        if (tags != null && !tags.isEmpty()) {
            builder.tags(tags);
        }
        
        // Set category attributes if present
        if (request.getCategoryType() != null) {
            Map<String, String> attrs = request.getAttributes();
            builder.attributes(attrs != null ? new LinkedHashMap<>(attrs) : new LinkedHashMap<>());
        }
        
        return builder.build();
    }
    
    /**
     * Generate or use existing friendly URL with uniqueness guarantee (CRITICAL-002 FIX).
     */
    public String generateOrEnsureUniqueFriendlyUrl(ProductCreateRequest request) {
        String friendlyUrl = request.getFriendlyUrl();
        
        if (!StringUtils.hasText(friendlyUrl)) {
            friendlyUrl = generateFriendlyUrl(request.getName());
        }
        
        return ensureUniqueFriendlyUrl(friendlyUrl);
    }
    
    /**
     * Generate SEO-friendly URL from product name.
     * Lowercases, removes special chars, converts spaces to hyphens.
     */
    public String generateFriendlyUrl(String name) {
        if (!StringUtils.hasText(name)) {
            return "product";
        }
        
        return name.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }
    
    /**
     * Ensure friendly URL is unique with circuit breaker (CRITICAL-002 FIX).
     * 
     * <p><b>Fixes:</b>
     * <ul>
     *   <li>Added upper bound (max 1000 attempts)</li>
     *   <li>UUID fallback if all attempts exhausted</li>
     *   <li>Exception if even UUID fails (extremely rare)</li>
     * </ul>
     * 
     * @param baseUrl the base URL to make unique
     * @return guaranteed unique URL
     * @throws FriendlyUrlGenerationException if unable to generate unique URL
     */
    public String ensureUniqueFriendlyUrl(String baseUrl) {
        // Quick check: is base URL already unique?
        if (!productRepository.existsByFriendlyUrl(baseUrl)) {
            return baseUrl;
        }
        
        // Try numbered suffixes
        int maxAttempts = productProperties.getMaxFriendlyUrlAttempts();
        
        for (int counter = 1; counter <= maxAttempts; counter++) {
            String friendlyUrl = String.format("%s-%d", baseUrl, counter);
            
            if (!productRepository.existsByFriendlyUrl(friendlyUrl)) {
                log.debug("Generated unique friendly URL: {} (attempt {})", friendlyUrl, counter);
                return friendlyUrl;
            }
        }
        
        // Fallback: append UUID suffix
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        String friendlyUrl = String.format("%s-%s", baseUrl, uniqueSuffix);
        
        if (productRepository.existsByFriendlyUrl(friendlyUrl)) {
            // Extremely rare case: even UUID collision
            throw new FriendlyUrlGenerationException(
                "Failed to generate unique friendly URL for: " + baseUrl + 
                " after " + maxAttempts + " attempts and UUID fallback");
        }
        
        log.warn("Friendly URL required UUID fallback: {}", friendlyUrl);
        return friendlyUrl;
    }
    
    /**
     * Batch resolve or create tags (MEDIUM-005 FIX).
     * 
     * <p>Performance: N+1 queries → 2 queries (findByNameIn + saveAll)
     * 
     * @param tagNames tag names to resolve/create
     * @return set of Tag entities
     */
    public Set<Tag> resolveOrCreateTags(Set<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return Collections.emptySet();
        }
        
        // Normalize tag names
        Set<String> normalizedNames = tagNames.stream()
            .filter(StringUtils::hasText)
            .map(String::toLowerCase)
            .map(String::trim)
            .collect(Collectors.toSet());
        
        if (normalizedNames.isEmpty()) {
            return Collections.emptySet();
        }
        
        // Fetch all existing tags in single query
        Set<Tag> existingTags = tagRepository.findByNameIn(normalizedNames);
        
        Set<String> existingNames = existingTags.stream()
            .map(Tag::getName)
            .collect(Collectors.toSet());
        
        // Find missing tags
        Set<String> missingNames = normalizedNames.stream()
            .filter(name -> !existingNames.contains(name))
            .collect(Collectors.toSet());
        
        // Batch create missing tags
        List<Tag> newTags = missingNames.stream()
            .map(name -> Tag.builder().name(name).build())
            .collect(Collectors.toList());
        
        if (!newTags.isEmpty()) {
            newTags = tagRepository.saveAll(newTags);
            log.debug("Created {} new tags: {}", newTags.size(), missingNames);
        }
        
        // Combine and return
        Set<Tag> allTags = new HashSet<>(existingTags);
        allTags.addAll(newTags);
        
        return allTags;
    }
    
    /**
     * Convert category attributes to Map<String, String> for persistence.
     */
    @SuppressWarnings("unchecked")
    public Map<String, String> convertToStringMap(Object attributes) {
        if (attributes == null) {
            return null;
        }
        Map<String, Object> rawMap = objectMapper.convertValue(attributes, Map.class);
        Map<String, String> stringMap = new HashMap<>();
        for (Map.Entry<String, Object> entry : rawMap.entrySet()) {
            stringMap.put(entry.getKey(), entry.getValue() != null ? entry.getValue().toString() : null);
        }
        return stringMap;
    }

    /**
     * Sanitize string content by HTML escaping.
     */
    public String sanitize(String content) {
        if (content == null) {
            return null;
        }
        return org.springframework.web.util.HtmlUtils.htmlEscape(content);
    }
}
