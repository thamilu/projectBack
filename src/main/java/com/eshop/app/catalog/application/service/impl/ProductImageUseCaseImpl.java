package com.eshop.app.catalog.application.service.impl;

import com.eshop.app.catalog.api.request.ProductImageRequest;
import com.eshop.app.catalog.api.response.ProductImageResponse;
import com.eshop.app.catalog.application.port.in.ProductImageUseCase;
import com.eshop.app.catalog.domain.entity.Product;
import com.eshop.app.catalog.domain.entity.ProductImage;
import com.eshop.app.catalog.domain.repository.ProductImageRepository;
import com.eshop.app.catalog.domain.repository.ProductRepository;
import com.eshop.app.catalog.application.mapper.ProductImageMapper;
import com.eshop.app.core.exception.business.ConflictException;
import com.eshop.app.storage.exception.ImageUploadException;
import com.eshop.app.core.exception.business.ResourceNotFoundException;
import com.eshop.app.storage.api.response.ImageUploadResult;
import com.eshop.app.storage.application.port.in.ImageStorageUseCase;
import com.eshop.app.storage.infrastructure.config.ImageStorageFactory;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductImageUseCaseImpl implements ProductImageUseCase {

    private final ProductImageRepository imageRepository;
    private final ProductRepository productRepository;
    private final ProductImageMapper productImageMapper;
    private final ImageStorageFactory storageFactory;

    @Override
    public ProductImageResponse addProductImage(ProductImageRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(
                        () -> new ResourceNotFoundException("Product not found with id: " + request.getProductId()));

        if (request.getIsPrimary() != null && request.getIsPrimary()) {
            unsetOtherPrimaryImages(product.getId());
        }

        Integer displayOrder = request.getDisplayOrder();
        if (displayOrder == null) {
            displayOrder = getNextDisplayOrder(product.getId());
        }

        ProductImage image = ProductImage.builder()
                .product(product)
                .url(request.getImageUrl())
                .altText(request.getAltText())
                .isPrimary(request.getIsPrimary() != null ? request.getIsPrimary() : false)
                .sortOrder(displayOrder)
                .active(true)
                .build();

        ProductImage savedImage = imageRepository.save(image);
        return productImageMapper.toProductImageResponse(savedImage);
    }

    @Override
    public ProductImageResponse updateProductImage(Long imageId, ProductImageRequest request) {
        ProductImage image = imageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Product image not found with id: " + imageId));

        if (request.getIsPrimary() != null && request.getIsPrimary() && !image.getIsPrimary()) {
            unsetOtherPrimaryImages(image.getProduct().getId());
        }

        image.setUrl(request.getImageUrl());
        image.setAltText(request.getAltText());
        if (request.getIsPrimary() != null) {
            image.setIsPrimary(request.getIsPrimary());
        }
        if (request.getDisplayOrder() != null) {
            image.setSortOrder(request.getDisplayOrder());
        }

        ProductImage updatedImage = imageRepository.save(image);
        return productImageMapper.toProductImageResponse(updatedImage);
    }

    @Override
    public void deleteProductImage(Long imageId) {
        ProductImage image = imageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Product image not found with id: " + imageId));
        try {
            if (image.getProvider() != null && image.getPublicId() != null) {
                ImageStorageUseCase storage = storageFactory.get();
                storage.delete(image.getPublicId(), "products/" + image.getProduct().getId());
            }
        } catch (Exception e) {
            System.err.println("Failed to delete remote image: " + e.getMessage());
        }

        image.setActive(false);
        imageRepository.save(image);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductImageResponse getProductImageById(Long imageId) {
        ProductImage image = imageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Product image not found with id: " + imageId));

        if (!image.getActive()) {
            throw new ResourceNotFoundException("Product image not found with id: " + imageId);
        }

        return productImageMapper.toProductImageResponse(image);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductImageResponse> getProductImages(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product not found with id: " + productId);
        }

        List<ProductImage> images = imageRepository.findByProductIdAndActiveTrueOrderByDisplayOrderAsc(productId);

        return images.stream()
                .map(productImageMapper::toProductImageResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ProductImageResponse setPrimaryImage(Long productId, Long imageId) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product not found with id: " + productId);
        }

        ProductImage image = imageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Product image not found with id: " + imageId));

        if (!image.getProduct().getId().equals(productId)) {
            throw new ConflictException("Image does not belong to the specified product");
        }

        if (!image.getActive()) {
            throw new ConflictException("Cannot set inactive image as primary");
        }

        unsetOtherPrimaryImages(productId);

        image.setIsPrimary(true);
        ProductImage updatedImage = imageRepository.save(image);

        return productImageMapper.toProductImageResponse(updatedImage);
    }

    private void unsetOtherPrimaryImages(Long productId) {
        List<ProductImage> images = imageRepository.findByProductIdAndActiveTrue(productId);
        images.stream()
                .filter(ProductImage::getIsPrimary)
                .forEach(img -> {
                    img.setIsPrimary(false);
                    imageRepository.save(img);
                });
    }

    private Integer getNextDisplayOrder(Long productId) {
        List<ProductImage> images = imageRepository.findByProductIdAndActiveTrue(productId);
        return images.stream()
                .map(ProductImage::getSortOrder)
                .filter(order -> order != null)
                .max(Comparator.naturalOrder())
                .map(max -> max + 1)
                .orElse(1);
    }

    @Override
    public ProductImageResponse uploadProductImage(Long productId, MultipartFile file, String altText,
            Boolean isPrimary) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        if (isPrimary != null && isPrimary) {
            unsetOtherPrimaryImages(product.getId());
        }

        Integer displayOrder = getNextDisplayOrder(product.getId());

        try {
            byte[] bytes = file.getBytes();
            String folder = "products/" + productId;
            ImageStorageUseCase storage = storageFactory.get();
            ImageUploadResult r = storage.upload(bytes, file.getOriginalFilename(), folder);

            ProductImage image = ProductImage.builder()
                    .product(product)
                    .url(r.getUrl())
                    .thumbnailUrl(r.getThumbnailUrl())
                    .publicId(r.getPublicId())
                    .provider(storage.getClass().getSimpleName())
                    .width(r.getWidth())
                    .height(r.getHeight())
                    .fileSize(r.getFileSize())
                    .altText(altText)
                    .isPrimary(isPrimary != null ? isPrimary : false)
                    .sortOrder(displayOrder)
                    .active(true)
                    .build();

            ProductImage savedImage = imageRepository.save(image);
            return productImageMapper.toProductImageResponse(savedImage);

        } catch (IOException e) {
            throw new ImageUploadException("Failed to upload image: " + e.getMessage(), e);
        }
    }
}

