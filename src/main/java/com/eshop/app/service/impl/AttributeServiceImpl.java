package com.eshop.app.service.impl;

import com.eshop.app.entity.AttributeDefinition;
import com.eshop.app.entity.CategoryAttributeMapping;
import com.eshop.app.entity.Category;
import com.eshop.app.repository.AttributeDefinitionRepository;
import com.eshop.app.repository.CategoryAttributeMappingRepository;
import com.eshop.app.repository.CategoryRepository;
import com.eshop.app.service.AttributeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AttributeServiceImpl implements AttributeService {

    private final AttributeDefinitionRepository attributeDefinitionRepository;
    private final CategoryAttributeMappingRepository categoryAttributeMappingRepository;
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public AttributeDefinition saveAttributeDefinition(AttributeDefinition definition) {
        return attributeDefinitionRepository.save(definition);
    }

    @Override
    public List<AttributeDefinition> getAllAttributeDefinitions() {
        return attributeDefinitionRepository.findAll();
    }

    @Override
    @Transactional
    public CategoryAttributeMapping mapAttributeToCategory(Long categoryId, Long attributeId, boolean required,
            int displayOrder) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        AttributeDefinition attribute = attributeDefinitionRepository.findById(attributeId)
                .orElseThrow(() -> new IllegalArgumentException("Attribute not found"));

        Optional<CategoryAttributeMapping> existing = categoryAttributeMappingRepository
                .findByCategoryIdAndAttributeDefinitionId(categoryId, attributeId);

        if (existing.isPresent()) {
            CategoryAttributeMapping mapping = existing.get();
            mapping.setRequired(required);
            mapping.setDisplayOrder(displayOrder);
            return categoryAttributeMappingRepository.save(mapping);
        }

        CategoryAttributeMapping mapping = CategoryAttributeMapping.builder()
                .category(category)
                .attributeDefinition(attribute)
                .required(required)
                .displayOrder(displayOrder)
                .build();

        return categoryAttributeMappingRepository.save(mapping);
    }

    @Override
    public List<CategoryAttributeMapping> getAttributesByCategory(Long categoryId) {
        return categoryAttributeMappingRepository.findByCategoryIdOrderByDisplayOrderAsc(categoryId);
    }

    @Override
    public void validateProductAttributes(Long categoryId, Map<String, String> attributes) {
        List<CategoryAttributeMapping> requiredMappings = categoryAttributeMappingRepository
                .findByCategoryIdOrderByDisplayOrderAsc(categoryId);

        for (CategoryAttributeMapping mapping : requiredMappings) {
            String attrName = mapping.getAttributeDefinition().getName();
            String value = attributes.get(attrName);

            // 1. Check Required
            if (mapping.getRequired() && (value == null || value.trim().isEmpty())) {
                throw new IllegalArgumentException(
                        "Missing required attribute: " + mapping.getAttributeDefinition().getLabel());
            }

            // 2. Validate Type (if value exists)
            if (value != null && !value.isEmpty()) {
                validateAttributeType(mapping.getAttributeDefinition(), value);
            }
        }
    }

    private void validateAttributeType(AttributeDefinition definition, String value) {
        switch (definition.getDataType()) {
            case NUMBER:
                try {
                    Double.parseDouble(value);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Attribute " + definition.getLabel() + " must be a number");
                }
                break;
            case DATE:
                try {
                    LocalDate.parse(value);
                } catch (DateTimeParseException e) {
                    throw new IllegalArgumentException(
                            "Attribute " + definition.getLabel() + " must be a valid date (YYYY-MM-DD)");
                }
                break;
            case BOOLEAN:
                if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")
                        && !value.equalsIgnoreCase("yes") && !value.equalsIgnoreCase("no")) {
                    throw new IllegalArgumentException(
                            "Attribute " + definition.getLabel() + " must be a boolean (true/false or yes/no)");
                }
                break;
            case TEXT:
            case SELECT:
            case MULTISELECT:
                // For now, we trust the value string.
                // Future improvement: Parse definition.getOptions() JSON and validate against
                // it.
                break;
            default:
                break;
        }
    }
}
