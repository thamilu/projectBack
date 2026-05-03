package com.eshop.app.service.impl;

import com.eshop.app.dto.request.ProductImageRequest;
import com.eshop.app.dto.response.ProductImageResponse;
import com.eshop.app.entity.Product;
import com.eshop.app.entity.ProductImage;
import com.eshop.app.exception.ConflictException;
import com.eshop.app.exception.ResourceNotFoundException;
import com.eshop.app.exception.ImageUploadException;
import com.eshop.app.mapper.EntityMapper;
import com.eshop.app.repository.ProductImageRepository;
import com.eshop.app.repository.ProductRepository;
import com.eshop.app.service.ProductImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Cloudinary is used via ImageStorageService implementations
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductImageServiceImpl implements ProductImageService {
    
    private final ProductImageRepository imageRepository;
    private final ProductRepository productRepository;
    private final EntityMapper entityMapper;
    private final com.eshop.app.storage.ImageStorageFactory storageFactory;
    
    @Override
    public ProductImageResponse addProductImage(ProductImageRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.getProductId()));
        
        // If this is set as primary, unset other primary images
        if (request.getIsPrimary() != null && request.getIsPrimary()) {
            unsetOtherPrimaryImages(product.getId());
        }
        
        // Set display order if not provided
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
        return entityMapper.toProductImageResponse(savedImage);
    }
    
    @Override
    public ProductImageResponse updateProductImage(Long imageId, ProductImageRequest request) {
        ProductImage image = imageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Product image not found with id: " + imageId));
        
        // If this is being set as primary, unset other primary images
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
        return entityMapper.toProductImageResponse(updatedImage);
    }
    
    @Override
    public void deleteProductImage(Long imageId) {
        ProductImage image = imageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Product image not found with id: " + imageId));
        // Attempt to delete remote asset if we have provider and publicId
        try {
            if (image.getProvider() != null && image.getPublicId() != null) {
                com.eshop.app.storage.ImageStorageService storage = storageFactory.get();
                // For Bunny provider we stored path as publicId; for Cloudinary it's the public id
                storage.delete(image.getPublicId(), "products/" + image.getProduct().getId());
            }
        } catch (Exception e) {
            // Log and continue with soft-delete; do not fail user request because remote deletion failed
            // Use System.err here to avoid adding a logger in this patch
            System.err.println("Failed to delete remote image: " + e.getMessage());
        }

        image.setActive(false); // Soft delete
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
        
        return entityMapper.toProductImageResponse(image);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ProductImageResponse> getProductImages(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product not found with id: " + productId);
        }
        
        List<ProductImage> images = imageRepository.findByProductIdAndActiveTrueOrderByDisplayOrderAsc(productId);
        
        return images.stream()
                .map(entityMapper::toProductImageResponse)
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
        
        // Unset other primary images
        unsetOtherPrimaryImages(productId);
        
        // Set this image as primary
        image.setIsPrimary(true);
        ProductImage updatedImage = imageRepository.save(image);
        
        return entityMapper.toProductImageResponse(updatedImage);
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
    public ProductImageResponse uploadProductImage(Long productId, MultipartFile file, String altText, Boolean isPrimary) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        if (isPrimary != null && isPrimary) {
            unsetOtherPrimaryImages(product.getId());
        }

        Integer displayOrder = getNextDisplayOrder(product.getId());

        try {
                byte[] bytes = file.getBytes();
                String folder = "products/" + productId;
                com.eshop.app.storage.ImageStorageService storage = storageFactory.get();
                com.eshop.app.storage.ImageUploadResult r = storage.upload(bytes, file.getOriginalFilename(), folder);

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
                return entityMapper.toProductImageResponse(savedImage);

        } catch (IOException e) {
            throw new ImageUploadException("Failed to upload image: " + e.getMessage(), e);
        }
    }
}