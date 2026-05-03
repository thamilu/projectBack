package com.eshop.app.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service("cloudinaryStorageService")
public class CloudinaryStorageService implements ImageStorageService {

    private final Cloudinary cloudinary;

    public CloudinaryStorageService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    @Override
        public ImageUploadResult upload(byte[] bytes, String filename, String folder) throws IOException {
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> options = (java.util.Map<String, Object>) ObjectUtils.asMap(
            "folder", folder,
            "resource_type", "image",
            "transformation", new Transformation<>().quality("auto").fetchFormat("auto")
        );

        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> result = (java.util.Map<String, Object>) cloudinary.uploader().upload(bytes, options);

        String publicId = (String) result.get("public_id");
        String url = (String) result.get("secure_url");
        Integer width = result.get("width") != null ? ((Number) result.get("width")).intValue() : null;
        Integer height = result.get("height") != null ? ((Number) result.get("height")).intValue() : null;
        Long fileSize = result.get("bytes") != null ? ((Number) result.get("bytes")).longValue() : null;

        String thumbnail = cloudinary.url()
            .transformation(new Transformation<>().width(150).height(150).crop("thumb").gravity("auto"))
            .generate(publicId);

        return ImageUploadResult.builder()
            .publicId(publicId)
            .url(url)
            .thumbnailUrl(thumbnail)
            .width(width)
            .height(height)
            .fileSize(fileSize)
            .build();
    }

    @Override
    public void delete(String publicId, String folder) throws IOException {
        cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "image"));
    }
}
