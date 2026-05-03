package com.eshop.app.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.OutputStream;

@Service("bunnyStorageService")
public class BunnyNetStorageService implements ImageStorageService {

    @Value("${bunny.storage.zone:}")
    private String storageZone;

    @Value("${bunny.api.key:}")
    private String apiKey;

    @Value("${bunny.cdn.hostname:}")
    private String cdnHostname; // e.g. myzone.b-cdn.net

    private String storageBase() {
        return "https://storage.bunnycdn.com/" + storageZone + "/";
    }

    private String publicUrl(String path) {
        if (cdnHostname != null && !cdnHostname.isBlank()) {
            return "https://" + cdnHostname + "/" + path;
        }
        return "https://" + storageZone + ".storage.bunnycdn.com/" + path;
    }

    @Override
    public ImageUploadResult upload(byte[] bytes, String filename, String folder) throws IOException {
        String path = (folder != null && !folder.isEmpty() ? folder + "/" : "") + filename;
        URL url = java.net.URI.create(storageBase() + path).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("PUT");
        conn.setRequestProperty("AccessKey", apiKey);
        conn.setRequestProperty("Content-Type", "application/octet-stream");
        conn.setFixedLengthStreamingMode(bytes.length);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(bytes);
        }
        int code = conn.getResponseCode();
        if (code >= 200 && code < 300) {
            String urlPublic = publicUrl(path);
            // Bunny.net doesn't provide width/height here; we return fileSize and same url for thumbnail
            return ImageUploadResult.builder()
                    .publicId(path)
                    .url(urlPublic)
                    .thumbnailUrl(urlPublic)
                    .width(null)
                    .height(null)
                    .fileSize((long) bytes.length)
                    .build();
        } else {
            throw new IOException("Bunny.net upload failed with HTTP code: " + code);
        }
    }

    @Override
    public void delete(String publicId, String folder) throws IOException {
        String path = publicId; // publicId was stored as path
        URL url = java.net.URI.create(storageBase() + path).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("DELETE");
        conn.setRequestProperty("AccessKey", apiKey);
        int code = conn.getResponseCode();
        if (code < 200 || code >= 300) {
            throw new IOException("Bunny.net delete failed with HTTP code: " + code);
        }
    }
}
