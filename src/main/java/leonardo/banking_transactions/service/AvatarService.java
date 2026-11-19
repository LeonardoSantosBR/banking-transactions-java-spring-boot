package leonardo.banking_transactions.service;

import java.io.IOException;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import leonardo.banking_transactions.service.aws.S3Service;

@Service
public class AvatarService {
    private static final long MAX_AVATAR_SIZE_BYTES = 5 * 1024 * 1024;

    private final S3Service s3Service;

    public AvatarService(S3Service s3Service) {
        this.s3Service = s3Service;
    }

    private void validate(MultipartFile avatar) {
        if (avatar.getContentType() == null || !avatar.getContentType().startsWith("image/")) {
            throw new IllegalArgumentException("Avatar must be an image");
        }
        if (avatar.getSize() > MAX_AVATAR_SIZE_BYTES) {
            throw new IllegalArgumentException("Avatar must not exceed 5 MB");
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "image";
        }
        return filename.substring(filename.lastIndexOf('.') + 1)
                .replaceAll("[^a-zA-Z0-9]", "")
                .toLowerCase();
    }

    public String upload(MultipartFile avatar) {
        validate(avatar);
        String objectKey = "avatars/" + UUID.randomUUID() + "/" + getFileExtension(avatar.getOriginalFilename());
        try {
            s3Service.upload(objectKey, avatar.getBytes(), avatar.getContentType());
            return objectKey;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read avatar file", exception);
        }
    }
}
