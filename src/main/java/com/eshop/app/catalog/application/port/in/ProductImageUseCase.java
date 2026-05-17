package com.eshop.app.catalog.application.port.in;

import com.eshop.app.catalog.api.request.ProductImageRequest;
import com.eshop.app.catalog.api.response.ProductImageResponse;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

/**
 * Inbound Port for Product Image Use Cases.
 */
public interface ProductImageUseCase {
    ProductImageResponse addProductImage(ProductImageRequest request);
    ProductImageResponse uploadProductImage(Long productId, MultipartFile file, String altText, Boolean isPrimary);
    ProductImageResponse updateProductImage(Long imageId, ProductImageRequest request);
    void deleteProductImage(Long imageId);
    ProductImageResponse getProductImageById(Long imageId);
    List<ProductImageResponse> getProductImages(Long productId);
    ProductImageResponse setPrimaryImage(Long productId, Long imageId);
}
