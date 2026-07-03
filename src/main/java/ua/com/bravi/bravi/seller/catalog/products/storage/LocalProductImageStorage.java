package ua.com.bravi.bravi.seller.catalog.products.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ua.com.bravi.bravi.seller.catalog.products.config.props.ProductImageStorageProperties;
import ua.com.bravi.bravi.shared.exception.NotFoundException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class LocalProductImageStorage implements ProductImageStorage {

    private final ProductImageStorageProperties properties;

    @Override
    public String store(byte[] content, String contentType, String originalFilename) {
        String key = UUID.randomUUID() + extension(originalFilename, contentType);
        Path target = resolve(key);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store product image", e);
        }
        return key;
    }

    @Override
    public byte[] load(String key) {
        Path source = resolve(key);
        if (!Files.exists(source)) {
            throw new NotFoundException("Image content not found");
        }
        try {
            return Files.readAllBytes(source);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read product image", e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to delete product image", e);
        }
    }

    /** Резолвить ключ під базовим каталогом і захищає від path traversal. */
    private Path resolve(String key) {
        Path base = Path.of(properties.getBasePath()).toAbsolutePath().normalize();
        Path resolved = base.resolve(key).normalize();
        if (!resolved.startsWith(base)) {
            throw new IllegalArgumentException("Invalid storage key");
        }
        return resolved;
    }

    private static String extension(String originalFilename, String contentType) {
        if (originalFilename != null) {
            int dot = originalFilename.lastIndexOf('.');
            if (dot >= 0 && dot < originalFilename.length() - 1) {
                return "." + originalFilename.substring(dot + 1).toLowerCase();
            }
        }
        if (contentType != null) {
            return switch (contentType) {
                case "image/png" -> ".png";
                case "image/jpeg", "image/jpg" -> ".jpg";
                case "image/webp" -> ".webp";
                case "image/gif" -> ".gif";
                default -> "";
            };
        }
        return "";
    }
}
