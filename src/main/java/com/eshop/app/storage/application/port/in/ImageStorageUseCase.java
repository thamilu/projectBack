package com.eshop.app.storage.application.port.in;

import com.eshop.app.storage.api.response.ImageUploadResult;
import java.io.IOException;

public interface ImageStorageUseCase {
    ImageUploadResult upload(byte[] bytes, String filename, String folder) throws IOException;
    void delete(String publicId, String folder) throws IOException;
}
