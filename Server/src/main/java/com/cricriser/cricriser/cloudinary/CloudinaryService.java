package com.cricriser.cricriser.cloudinary;

import java.io.IOException;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public CloudinaryService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    public String uploadFile(MultipartFile file, String folder) throws IOException {
        try {
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap("folder", folder));
            return uploadResult.get("secure_url").toString();
        } catch (IOException e) {
            throw new IOException("Failed to upload file to Cloudinary: " + e.getMessage());
        }
    }

    public void deleteFile(String url) throws IOException {
        try {
            String publicId = extractPublicIdFromUrl(url);
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (IOException e) {
            throw new IOException("Failed to delete file from Cloudinary: " + e.getMessage());
        }
    }

    private String extractPublicIdFromUrl(String url) {
        // Example URL: https://res.cloudinary.com/your-cloud-name/image/upload/v1234567890/folder/filename.jpg
        String[] parts = url.split("/upload/");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid Cloudinary URL");
        }
        String publicIdWithVersion = parts[1];
        // Remove version prefix (e.g., v1234567890/) and file extension
        return publicIdWithVersion.substring(publicIdWithVersion.indexOf("/") + 1).replaceAll("\\.[^.]+$", "");
    }
}