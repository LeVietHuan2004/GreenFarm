package com.agri.ecommerce.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.agri.ecommerce.common.exception.ApplicationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImageStorageService {
    private static final Map<String, String> EXTENSIONS = Map.of(
        "image/jpeg", ".jpg", "image/png", ".png", "image/webp", ".webp"
    );
    private static final Set<String> FOLDERS = Set.of("catalog", "products", "avatars");
    private final Path root;
    private final long maxBytes;
    private final Cloudinary cloudinary;

    @Autowired
    public ImageStorageService(@Value("${app.upload.directory:uploads}") String directory,
                               @Value("${app.upload.max-image-bytes:5242880}") long maxBytes,
                               @Value("${app.upload.cloudinary.cloud-name:}") String cloudName,
                               @Value("${app.upload.cloudinary.api-key:}") String apiKey,
                               @Value("${app.upload.cloudinary.api-secret:}") String apiSecret) {
        this(directory, maxBytes, configuredCloudinary(cloudName, apiKey, apiSecret));
    }

    ImageStorageService(String directory, long maxBytes, Cloudinary cloudinary) {
        this.root = Path.of(directory).toAbsolutePath().normalize();
        this.maxBytes = maxBytes;
        this.cloudinary = cloudinary;
    }

    private static Cloudinary configuredCloudinary(String cloudName, String apiKey, String apiSecret) {
        boolean hasCloudName = StringUtils.hasText(cloudName);
        boolean hasApiKey = StringUtils.hasText(apiKey);
        boolean hasApiSecret = StringUtils.hasText(apiSecret);
        if ((hasCloudName || hasApiKey || hasApiSecret) && !(hasCloudName && hasApiKey && hasApiSecret)) {
            throw new IllegalStateException("Cloudinary requires CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY and CLOUDINARY_API_SECRET");
        }
        return hasCloudName ? new Cloudinary(ObjectUtils.asMap(
            "cloud_name", cloudName, "api_key", apiKey, "api_secret", apiSecret, "secure", true
        )) : null;
    }

    public String store(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) throw invalid("Vui lòng chọn một ảnh để tải lên");
        if (file.getSize() > maxBytes) throw invalid("Ảnh không được vượt quá 5 MB");
        String extension = EXTENSIONS.get(file.getContentType());
        if (extension == null) throw invalid("Chỉ hỗ trợ ảnh JPG, PNG hoặc WEBP");
        if (!FOLDERS.contains(folder)) throw invalid("Thư mục tải lên không hợp lệ");
        if (cloudinary != null) return storeInCloudinary(file, folder);

        Path targetDirectory = root.resolve(folder).normalize();
        String fileName = UUID.randomUUID() + extension;
        try {
            Files.createDirectories(targetDirectory);
            file.transferTo(targetDirectory.resolve(fileName));
            return "/uploads/" + folder + "/" + fileName;
        } catch (IOException exception) {
            throw new ApplicationException(HttpStatus.INTERNAL_SERVER_ERROR, "IMAGE_UPLOAD_FAILED", "Không thể lưu ảnh tải lên");
        }
    }

    private String storeInCloudinary(MultipartFile file, String folder) {
        try {
            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "folder", "greenfarm/" + folder,
                "resource_type", "image"
            ));
            Object secureUrl = result.get("secure_url");
            if (secureUrl instanceof String url && url.startsWith("https://") && url.length() <= 1024) {
                return url;
            }
            throw new ApplicationException(HttpStatus.BAD_GATEWAY, "IMAGE_UPLOAD_FAILED", "Cloudinary không trả về URL ảnh hợp lệ");
        } catch (IOException exception) {
            throw new ApplicationException(HttpStatus.BAD_GATEWAY, "IMAGE_UPLOAD_FAILED", "Không thể tải ảnh lên Cloudinary");
        }
    }

    private ApplicationException invalid(String message) {
        return new ApplicationException(HttpStatus.BAD_REQUEST, "INVALID_IMAGE", message);
    }
}
