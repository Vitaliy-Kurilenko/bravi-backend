package ua.com.bravi.bravi.shared.media;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import ua.com.bravi.bravi.shared.config.props.MediaStorageProperties;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class S3MediaStorage implements MediaStorage {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final MediaStorageProperties properties;

    @Override
    public PresignedUpload presignUpload(MediaUploadRequest request) {
        String key = request.category().keyPrefix(request.scope())
                + "/" + UUID.randomUUID() + extension(request.contentType(), request.originalFilename());
        PutObjectRequest putObject = PutObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(key)
                .contentType(request.contentType())
                .build();
        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(PutObjectPresignRequest.builder()
                .signatureDuration(properties.getPresignTtl())
                .putObjectRequest(putObject)
                .build());
        Map<String, String> headers = Map.of("Content-Type", request.contentType());
        return new PresignedUpload(presigned.url().toString(), key, headers, presigned.expiration());
    }

    @Override
    public Optional<StoredObject> stat(String key) {
        try {
            HeadObjectResponse head = s3Client.headObject(b -> b.bucket(properties.getBucket()).key(key));
            return Optional.of(new StoredObject(key, head.contentType(), head.contentLength()));
        } catch (NoSuchKeyException e) {
            return Optional.empty();
        }
    }

    @Override
    public void delete(String key) {
        s3Client.deleteObject(b -> b.bucket(properties.getBucket()).key(key));
    }

    @Override
    public String publicUrl(String key) {
        String base = properties.getPublicBaseUrl();
        return base.endsWith("/") ? base + key : base + "/" + key;
    }

    private static String extension(String contentType, String originalFilename) {
        if (contentType != null) {
            String ext = switch (contentType) {
                case "image/png" -> ".png";
                case "image/jpeg", "image/jpg" -> ".jpg";
                case "image/webp" -> ".webp";
                case "image/gif" -> ".gif";
                default -> null;
            };
            if (ext != null) {
                return ext;
            }
        }
        if (originalFilename != null) {
            int dot = originalFilename.lastIndexOf('.');
            if (dot >= 0 && dot < originalFilename.length() - 1) {
                return "." + originalFilename.substring(dot + 1).toLowerCase();
            }
        }
        return "";
    }
}
