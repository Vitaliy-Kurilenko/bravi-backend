package ua.com.bravi.bravi.shared.media;

/**
 * Запит на presigned-завантаження: логічна категорія (звідки береться префікс ключа й обмеження),
 * scope власника (напр. {@code "7"} або {@code "7/42"}) і метадані файлу.
 */
public record MediaUploadRequest(
        MediaCategory category,
        String scope,
        String contentType,
        long size,
        String originalFilename
) {
}
