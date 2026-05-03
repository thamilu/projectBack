package com.eshop.app.mapper.common;

import com.eshop.app.entity.*;
import com.eshop.app.exception.ResourceNotFoundException;
import com.eshop.app.repository.*;
import lombok.RequiredArgsConstructor;
import org.mapstruct.Named;
import org.mapstruct.TargetType;
import org.springframework.stereotype.Component;

import java.util.HashSet;

/**
 * Resolves entity references from IDs during mapping.
 */
@Component
@RequiredArgsConstructor
public class ReferenceMapper {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;
    private final TagRepository tagRepository;
    private final OrderRepository orderRepository;

    @Named("idToEntity")
    public <T> T resolve(Long id, @TargetType Class<T> entityClass) {
        if (id == null)
            return null;
        if (entityClass == User.class)
            return entityClass.cast(resolveUser(id));
        if (entityClass == Category.class)
            return entityClass.cast(resolveCategory(id));
        if (entityClass == Brand.class)
            return entityClass.cast(resolveBrand(id));
        if (entityClass == Store.class)
            return entityClass.cast(resolveStore(id));
        if (entityClass == Product.class)
            return entityClass.cast(resolveProduct(id));
        if (entityClass == Tag.class)
            return entityClass.cast(resolveTag(id));
        throw new IllegalArgumentException("Unknown entity class: " + entityClass.getName());
    }

    @Named("idToUser")
    public User resolveUser(Long id) {
        if (id == null)
            return null;
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }

    @Named("idToCategory")
    public Category resolveCategory(Long id) {
        if (id == null)
            return null;
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));
    }

    @Named("idToBrand")
    public Brand resolveBrand(Long id) {
        if (id == null)
            return null;
        return brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Brand", "id", id));
    }

    @Named("idToStore")
    public Store resolveStore(Long id) {
        if (id == null)
            return null;
        return storeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Store", "id", id));
    }

    @Named("idToProduct")
    public Product resolveProduct(Long id) {
        if (id == null)
            return null;
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
    }

    @Named("idToTag")
    public Tag resolveTag(Long id) {
        if (id == null)
            return null;
        return tagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tag", "id", id));
    }

    @Named("idToOrder")
    public Order resolveOrder(Long id) {
        if (id == null)
            return null;
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));
    }

    @Named("idsToTags")
    public java.util.Set<Tag> resolveTags(java.util.Set<Long> ids) {
        if (ids == null || ids.isEmpty())
            return new HashSet<>();
        return new HashSet<>(tagRepository.findAllById(ids));
    }

    @Named("idsToProducts")
    public java.util.Set<Product> resolveProducts(java.util.Set<Long> ids) {
        if (ids == null || ids.isEmpty())
            return new HashSet<>();
        return new HashSet<>(productRepository.findAllById(ids));
    }

    @Named("entityToId")
    public Long toId(com.eshop.app.entity.BaseEntity entity) {
        return entity != null ? entity.getId() : null;
    }

    @Named("userToId")
    public Long toUserId(User user) {
        return user != null ? user.getId() : null;
    }

    @Named("categoryToId")
    public Long toCategoryId(Category category) {
        return category != null ? category.getId() : null;
    }

    @Named("brandToId")
    public Long toBrandId(Brand brand) {
        return brand != null ? brand.getId() : null;
    }

    @Named("storeToId")
    public Long toStoreId(Store store) {
        return store != null ? store.getId() : null;
    }

    @Named("tagsToIds")
    public java.util.Set<Long> toTagIds(java.util.Set<Tag> tags) {
        if (tags == null)
            return null;
        return tags.stream().map(Tag::getId).collect(java.util.stream.Collectors.toSet());
    }
}
