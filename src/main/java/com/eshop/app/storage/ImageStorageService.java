package com.eshop.app.storage;

import java.io.IOException;

public interface ImageStorageService {
    ImageUploadResult upload(byte[] bytes, String filename, String folder) throws IOException;
    void delete(String publicId, String folder) throws IOException;
}
