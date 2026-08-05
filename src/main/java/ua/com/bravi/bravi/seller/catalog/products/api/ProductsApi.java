package ua.com.bravi.bravi.seller.catalog.products.api;

import ua.com.bravi.bravi.seller.catalog.products.domain.Product;
import ua.com.bravi.bravi.seller.catalog.products.domain.ProductSearchQuery;
import ua.com.bravi.bravi.shared.media.PresignedUpload;

import java.util.List;

public interface ProductsApi {

    ProductPage search(Long storeId, ProductSearchQuery query);

    /** Internal lookup by bigint id, used by cross-module consumers such as orders. */
    ProductView getById(Long storeId, Long productId);

    ProductView getByPublicId(Long storeId, String publicId);

    ProductView create(Long storeId, Product product);

    void update(Long storeId, String publicId, Product patch);

    void delete(Long storeId, String publicId);

    List<ProductImageView> listImages(Long storeId, String publicId);

    /** Validates the declared image and returns a presigned PUT URL for direct client upload. */
    PresignedUpload presignImageUpload(Long storeId, String publicId, ImageUpload upload);

    /** Confirms an uploaded image: re-validates the stored object and appends it to the gallery. */
    ProductImageView confirmImage(Long storeId, String publicId, String storageKey);

    /**
     * Moves an image to a zero-based position, shifting the others, and returns the whole
     * re-sequenced gallery. Position 0 makes the image the product's main one.
     */
    List<ProductImageView> moveImage(Long storeId, String publicId, Long imageId, int sortOrder);

    void deleteImage(Long storeId, String publicId, Long imageId);
}
