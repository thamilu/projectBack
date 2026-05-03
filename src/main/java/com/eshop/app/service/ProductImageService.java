package com.eshop.app.service;

import com.eshop.app.dto.request.ProductImageRequest;
import com.eshop.app.dto.response.ProductImageResponse;

import java.util.List;

public interface ProductImageService {
    
    ProductImageResponse addProductImage(ProductImageRequest request);
    
    ProductImageResponse uploadProductImage(Long productId, org.springframework.web.multipart.MultipartFile file, String altText, Boolean isPrimary);
    
    ProductImageResponse updateProductImage(Long imageId, ProductImageRequest request);
    
    void deleteProductImage(Long imageId);
    
    ProductImageResponse getProductImageById(Long imageId);
    
    List<ProductImageResponse> getProductImages(Long productId);
    
    ProductImageResponse setPrimaryImage(Long productId, Long imageId);
}