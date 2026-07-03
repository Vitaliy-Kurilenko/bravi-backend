package ua.com.bravi.bravi.seller.catalog.products.storage;

/**
 * Порт сховища зображень товарів. Реалізації (локальна ФС, у майбутньому S3/MinIO) приховані за
 * цим інтерфейсом, тож {@code ProductService} не залежить від конкретного бекенда.
 */
public interface ProductImageStorage {

    /** Зберігає вміст і повертає storage key (для збереження у БД). */
    String store(byte[] content, String contentType, String originalFilename);

    /** Читає вміст за ключем. */
    byte[] load(String key);

    /** Видаляє об'єкт за ключем (ідемпотентно). */
    void delete(String key);
}
