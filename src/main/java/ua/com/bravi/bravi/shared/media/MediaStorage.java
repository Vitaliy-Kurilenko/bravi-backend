package ua.com.bravi.bravi.shared.media;

import java.util.Optional;

/**
 * Порт об'єктного сховища медіа. Клієнт вантажить файл напряму в сховище за presigned-посиланням,
 * тож байти не проходять крізь застосунок. Реалізації (S3/MinIO) приховані за цим інтерфейсом.
 */
public interface MediaStorage {

    /** Генерує storage key під заданим префіксом і повертає presigned PUT URL для прямого завантаження. */
    PresignedUpload presignUpload(MediaUploadRequest request);

    /** Метадані об'єкта за ключем (HEAD); {@code empty}, якщо об'єкта немає. */
    Optional<StoredObject> stat(String key);

    /** Видаляє об'єкт за ключем (ідемпотентно). */
    void delete(String key);

    /** Стабільний публічний URL об'єкта (bucket/CDN із public-read). */
    String publicUrl(String key);
}
