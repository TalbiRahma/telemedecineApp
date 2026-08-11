package com.telemedecine.api.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.telemedecine.api.exception.CertificationUploadConfigurationException;
import com.telemedecine.api.exception.CertificationUploadException;
import com.telemedecine.api.service.CloudinaryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class CloudinaryServiceImpl implements CloudinaryService {

    private static final Set<String> ACCEPTED_CONTENT_TYPES = Set.of(
            "application/pdf", "image/jpeg", "image/jpg", "image/png"
    );
    private static final Set<String> ACCEPTED_EXTENSIONS = Set.of("pdf", "jpeg", "jpg", "png");

    private final Cloudinary cloudinary;
    private final String cloudName;
    private final String apiKey;
    private final String apiSecret;
    private final long maxFileSizeBytes;

    public CloudinaryServiceImpl(
            Cloudinary cloudinary,
            @Value("${cloudinary.cloud-name:}") String cloudName,
            @Value("${cloudinary.api-key:}") String apiKey,
            @Value("${cloudinary.api-secret:}") String apiSecret,
            @Value("${cloudinary.max-file-size-bytes:10485760}") long maxFileSizeBytes) {
        this.cloudinary = cloudinary;
        this.cloudName = cloudName;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    @Override
    public String uploadImage(MultipartFile file) {
        validateFile(file);
        validateConfiguration();

        try {
            Map<?, ?> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(), ObjectUtils.asMap("resource_type", "auto")
            );
            Object secureUrl = uploadResult.get("secure_url");
            if (!(secureUrl instanceof String url) || url.isBlank()) {
                throw new CertificationUploadException("The certification upload provider returned no file URL.");
            }
            return url;
        } catch (CertificationUploadException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new CertificationUploadException("Failed to upload doctor certification.", exception);
        }
    }

    private void validateConfiguration() {
        if (cloudName.isBlank() || apiKey.isBlank() || apiSecret.isBlank()) {
            throw new CertificationUploadConfigurationException(
                    "Doctor certification upload service is not configured."
            );
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("A certification document is required.");
        }
        if (file.getSize() > maxFileSizeBytes) {
            throw new IllegalArgumentException("The certification document exceeds the allowed file size.");
        }

        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        int extensionSeparator = filename.lastIndexOf('.');
        String extension = extensionSeparator < 0
                ? ""
                : filename.substring(extensionSeparator + 1).toLowerCase(Locale.ROOT);
        String contentType = file.getContentType() == null
                ? ""
                : file.getContentType().toLowerCase(Locale.ROOT);
        boolean genericContentType = contentType.isBlank() || "application/octet-stream".equals(contentType);

        if (!ACCEPTED_EXTENSIONS.contains(extension)
                || (!genericContentType && !ACCEPTED_CONTENT_TYPES.contains(contentType))) {
            throw new IllegalArgumentException("Certification must be a PDF, JPEG, JPG, or PNG file.");
        }
    }
}
