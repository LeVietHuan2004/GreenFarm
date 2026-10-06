package com.agri.ecommerce.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agri.ecommerce.common.exception.ApplicationException;
import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class ImageStorageServiceTest {
    @TempDir Path directory;

    @Test
    void cloudinaryUploadReturnsHttpsUrlWithoutCreatingLocalFile() throws Exception {
        Cloudinary cloudinary = mock(Cloudinary.class);
        Uploader uploader = mock(Uploader.class);
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), any(Map.class))).thenReturn(Map.of(
            "secure_url", "https://res.cloudinary.com/example/image/upload/greenfarm/products/photo.jpg"
        ));
        ImageStorageService service = new ImageStorageService(directory.toString(), 5242880, cloudinary);

        String url = service.store(new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[]{1, 2, 3}), "products");

        assertEquals("https://res.cloudinary.com/example/image/upload/greenfarm/products/photo.jpg", url);
        assertFalse(Files.exists(directory.resolve("products")));
        verify(uploader).upload(any(byte[].class), any(Map.class));
    }

    @Test
    void unsupportedFileIsRejectedBeforeCloudinaryCall() {
        ImageStorageService service = new ImageStorageService(directory.toString(), 5242880, mock(Cloudinary.class));

        ApplicationException error = assertThrows(ApplicationException.class,
            () -> service.store(new MockMultipartFile("file", "file.txt", "text/plain", new byte[]{1}), "catalog"));

        assertEquals("INVALID_IMAGE", error.getCode());
    }

    @Test
    void partialCloudinaryConfigurationFailsFast() {
        assertThrows(IllegalStateException.class,
            () -> new ImageStorageService(directory.toString(), 5242880, "cloud", "key", ""));
    }
}
