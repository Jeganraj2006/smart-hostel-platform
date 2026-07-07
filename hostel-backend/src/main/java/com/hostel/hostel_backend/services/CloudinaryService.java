package com.hostel.hostel_backend.services;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * Thin wrapper around the Cloudinary Java SDK.
 *
 * Configuration is read from application.properties:
 *   cloudinary.cloud-name, cloudinary.api-key, cloudinary.api-secret
 *
 * Cloudinary is initialized lazily on first use to avoid startup failures
 * when the environment variables are not set (e.g. in unit tests).
 */
@Service
public class CloudinaryService {

    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    @Value("${cloudinary.api-key}")
    private String apiKey;

    @Value("${cloudinary.api-secret}")
    private String apiSecret;

    private volatile Cloudinary cloudinary;

    private Cloudinary getClient() {
        if (cloudinary == null) {
            synchronized (this) {
                if (cloudinary == null) {
                    cloudinary = new Cloudinary(ObjectUtils.asMap(
                            "cloud_name", cloudName,
                            "api_key",    apiKey,
                            "api_secret", apiSecret,
                            "secure",     true
                    ));
                }
            }
        }
        return cloudinary;
    }

    /**
     * Uploads a multipart image to Cloudinary under the given folder and
     * returns the secure HTTPS URL of the uploaded asset.
     *
     * @param file   the image file from the HTTP multipart request
     * @param folder a Cloudinary folder path, e.g. "visitors" or "profiles"
     * @return       the secure_url of the uploaded image
     * @throws IOException if the upload or file reading fails
     */
    @SuppressWarnings("unchecked")
    public String uploadImage(MultipartFile file, String folder) throws IOException {
        Map<String, Object> params = ObjectUtils.asMap(
                "folder",         folder,
                "resource_type",  "image",
                "overwrite",      false
        );
        Map<String, Object> result = getClient().uploader()
                .upload(file.getBytes(), params);
        return (String) result.get("secure_url");
    }
}
