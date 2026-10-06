package com.agri.ecommerce.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import java.net.URI;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class CloudinaryImageStorageLiveTest {
    @TempDir Path directory;

    @Test
    @EnabledIfEnvironmentVariable(named = "GREENFARM_CLOUDINARY_LIVE_TEST", matches = "true")
    void uploadsImageThroughStorageServiceAndRemovesTestAsset() throws Exception {
        Cloudinary cloudinary = new Cloudinary(ObjectUtils.asMap(
            "cloud_name", System.getenv("CLOUDINARY_CLOUD_NAME"),
            "api_key", System.getenv("CLOUDINARY_API_KEY"),
            "api_secret", System.getenv("CLOUDINARY_API_SECRET"),
            "secure", true
        ));
        ImageStorageService service = new ImageStorageService(directory.toString(), 5242880, cloudinary);
        byte[] pixel = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO2vN9sAAAAASUVORK5CYII="
        );

        String url = service.store(new MockMultipartFile("file", "smoke.png", "image/png", pixel), "catalog");
        assertTrue(url.startsWith("https://res.cloudinary.com/"));

        String path = URI.create(url).getPath();
        String assetPath = path.substring(path.indexOf("/upload/") + "/upload/".length());
        String publicId = assetPath.replaceFirst("^v[0-9]+/", "").replaceFirst("\\.[^.]+$", "");
        Map<?, ?> deleted = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        assertEquals("ok", deleted.get("result"));
    }
}
