package ua.com.bravi.bravi.seller.catalog.products.api;

import ua.com.bravi.bravi.seller.catalog.products.domain.Product;
import ua.com.bravi.bravi.seller.catalog.products.domain.ProductSearchQuery;
import ua.com.bravi.bravi.shared.media.PresignedUpload;

import java.util.List;

public interface ProductsApi {

    ProductPage search(Long storeId, ProductSearchQuery query);

    /** Внутрішній lookup за bigint id — для крос-модульних споживачів (orders). */
    ProductView getById(Long storeId, Long productId);

    ProductView getByPublicId(Long storeId, String publicId);

    ProductView create(Long storeId, Product product);

    void update(Long storeId, String publicId, Product patch);

    void delete(Long storeId, String publicId);

    List<ProductImageView> listImages(Long storeId, String publicId);

    /** Validates the declared image and returns a presigned PUT URL for direct client upload. */
    PresignedUpload presignImageUpload(Long storeId, String publicId, ImageUpload upload);

    /** Confirms an uploaded image: re-validates the stored object and attaches it to the gallery. */
    ProductImageView confirmImage(Long storeId, String publicId, String storageKey, boolean primary);

    /** Makes the given image the gallery's primary one, demoting the others. */
    ProductImageView setPrimaryImage(Long storeId, String publicId, Long imageId);

    void deleteImage(Long storeId, String publicId, Long imageId);
}
