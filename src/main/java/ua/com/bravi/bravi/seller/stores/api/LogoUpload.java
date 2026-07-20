package ua.com.bravi.bravi.seller.stores.api;

/** Заявлені клієнтом метадані логотипу для presign (валідуються до і після завантаження). */
public record LogoUpload(
        String contentType,
        long size,
        String originalFilename
) {
}
