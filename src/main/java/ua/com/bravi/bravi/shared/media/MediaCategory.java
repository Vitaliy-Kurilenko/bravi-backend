package ua.com.bravi.bravi.shared.media;

import org.springframework.util.unit.DataSize;
import ua.com.bravi.bravi.shared.media.exception.InvalidMediaUploadException;

import java.util.Set;

/**
 * Реєстр логічних типів медіа: де об'єкт лежить у сховищі (префікс ключа) та які обмеження на файл.
 * Один bucket, розділення за префіксом; кожна сутність, що завантажує медіа, передає свою категорію.
 * Нові типи (фото товару, категорії тощо) додаються сюди — авторизація й attach лишаються в модулі-власнику.
 */
public enum MediaCategory {

    STORE_LOGO("store-logos", Set.of("image/png", "image/jpeg", "image/webp"), DataSize.ofMegabytes(5)),
    PRODUCT_IMAGE("product-images", Set.of("image/png", "image/jpeg", "image/webp"), DataSize.ofMegabytes(5));

    private static final String FIELD = "file";

    private final String prefix;
    private final Set<String> allowedContentTypes;
    private final long maxSizeBytes;

    MediaCategory(String prefix, Set<String> allowedContentTypes, DataSize maxSize) {
        this.prefix = prefix;
        this.allowedContentTypes = allowedContentTypes;
        this.maxSizeBytes = maxSize.toBytes();
    }

    /** Префікс ключа для конкретного власника, напр. {@code STORE_LOGO.keyPrefix("7") → "store-logos/7"}. */
    public String keyPrefix(String scope) {
        return prefix + "/" + scope;
    }

    /** Перевіряє тип і розмір; викликається на presign (заявлені) і на confirm (реальні з {@code stat}). */
    public void validate(String contentType, long size) {
        if (contentType == null || !allowedContentTypes.contains(contentType.toLowerCase())) {
            throw new InvalidMediaUploadException(FIELD, "Unsupported media type: " + contentType);
        }
        if (size <= 0) {
            throw new InvalidMediaUploadException(FIELD, "Empty file");
        }
        if (size > maxSizeBytes) {
            throw new InvalidMediaUploadException(FIELD, "File exceeds the maximum allowed size");
        }
    }
}
