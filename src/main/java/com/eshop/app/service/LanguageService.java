package com.eshop.app.service;

import com.eshop.app.dto.response.LanguageDTO;
import com.eshop.app.entity.*;
import com.eshop.app.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LanguageService {
    
    private final LanguageRepository languageRepository;
    private final ProductTranslationRepository productTranslationRepository;
    private final CategoryTranslationRepository categoryTranslationRepository;
    private final BrandTranslationRepository brandTranslationRepository;
    
    public Language createLanguage(Language language) {
        if (languageRepository.existsByCode(language.getCode())) {
            throw new IllegalArgumentException("Language with code " + language.getCode() + " already exists");
        }
        
        if (language.getIsDefault()) {
            clearDefaultLanguage();
        }
        
        return languageRepository.save(language);
    }
    
    public Language updateLanguage(Long id, Language language) {
        Language existing = languageRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Language not found"));
        
        existing.setName(language.getName());
        existing.setLocale(language.getLocale());
        existing.setRtl(language.getRtl());
        existing.setActive(language.getActive());
        existing.setSortOrder(language.getSortOrder());
        existing.setFlagIcon(language.getFlagIcon());
        
        if (language.getIsDefault() && !existing.getIsDefault()) {
            clearDefaultLanguage();
            existing.setIsDefault(true);
        }
        
        return languageRepository.save(existing);
    }
    
    @Cacheable("languages")
    public List<LanguageDTO> getAllLanguages() {
        return languageRepository.findAll().stream()
            .map(this::toDTO)
            .toList();
    }
    
    @Cacheable("languages")
    public List<LanguageDTO> getActiveLanguages() {
        return languageRepository.findByActiveTrueOrderBySortOrderAsc().stream()
            .map(this::toDTO)
            .toList();
    }
    
    @Cacheable(value = "languages", key = "#code")
    public LanguageDTO getLanguageByCode(String code) {
        Language language = languageRepository.findByCode(code)
            .orElseThrow(() -> new RuntimeException("Language not found: " + code));
        return toDTO(language);
    }
    
    @Cacheable(value = "languages", key = "'default'")
    public LanguageDTO getDefaultLanguage() {
        Language language = languageRepository.findByIsDefaultTrue()
            .orElseGet(() -> {
                log.warn("No default language found, creating English as default");
                return createLanguage(Language.builder()
                    .code("en")
                    .name("English")
                    .locale("en_US")
                    .rtl(false)
                    .isDefault(true)
                    .active(true)
                    .sortOrder(0)
                    .build());
            });
        return toDTO(language);
    }
    
    // Helper method to convert entity to DTO (safe for Redis)
    private LanguageDTO toDTO(Language language) {
        return new LanguageDTO(
            language.getId(),
            language.getCode(),
            language.getName(),
            language.getLocale(),
            language.getRtl(),
            language.getIsDefault(),
            language.getActive(),
            language.getSortOrder(),
            language.getFlagIcon()
        );
    }
    
    // Product Translations
    public ProductTranslation createProductTranslation(ProductTranslation translation) {
        return productTranslationRepository.save(translation);
    }
    
    public ProductTranslation getProductTranslation(Long productId, Long languageId) {
        return productTranslationRepository.findByProductIdAndLanguageId(productId, languageId)
            .orElse(null);
    }
    
    public List<ProductTranslation> getProductTranslations(Product product) {
        return productTranslationRepository.findByProduct(product);
    }
    
    // Category Translations
    public CategoryTranslation createCategoryTranslation(CategoryTranslation translation) {
        return categoryTranslationRepository.save(translation);
    }
    
    public CategoryTranslation getCategoryTranslation(Category category, Language language) {
        return categoryTranslationRepository.findByCategoryAndLanguage(category, language)
            .orElse(null);
    }
    
    public List<CategoryTranslation> getCategoryTranslations(Category category) {
        return categoryTranslationRepository.findByCategory(category);
    }
    
    // Brand Translations
    public BrandTranslation createBrandTranslation(BrandTranslation translation) {
        return brandTranslationRepository.save(translation);
    }
    
    public BrandTranslation getBrandTranslation(Brand brand, Language language) {
        return brandTranslationRepository.findByBrandAndLanguage(brand, language)
            .orElse(null);
    }
    
    public List<BrandTranslation> getBrandTranslations(Brand brand) {
        return brandTranslationRepository.findByBrand(brand);
    }
    
    private void clearDefaultLanguage() {
        languageRepository.findByIsDefaultTrue().ifPresent(language -> {
            language.setIsDefault(false);
            languageRepository.save(language);
        });
    }
    
    public void deleteLanguage(Long id) {
        Language language = languageRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Language not found"));
        
        if (language.getIsDefault()) {
            throw new IllegalStateException("Cannot delete default language");
        }
        
        languageRepository.delete(language);
    }
}
