package ua.com.bravi.bravi.seller.catalog.products.api;

import ua.com.bravi.bravi.seller.catalog.attributes.api.ProductAttributeValueView;
import ua.com.bravi.bravi.seller.catalog.attributes.api.ProductAttributesView;
import ua.com.bravi.bravi.seller.catalog.attributes.domain.AttributeValue;
import ua.com.bravi.bravi.seller.catalog.discounts.api.DiscountBulkResultView;
import ua.com.bravi.bravi.seller.catalog.discounts.api.DiscountView;
import ua.com.bravi.bravi.seller.catalog.discounts.domain.Discount;
import ua.com.bravi.bravi.seller.catalog.discounts.domain.SubmittedDiscount;
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

    /** What the product's category offers and what the product currently carries. */
    ProductAttributesView describeAttributes(Long storeId, String publicId);

    /** Replaces the product's whole set of characteristic values. */
    List<ProductAttributeValueView> replaceAttributes(Long storeId, String publicId, List<AttributeValue> values);

    /** Copies every characteristic value of another product of the same store onto this one. */
    List<ProductAttributeValueView> copyAttributesFrom(Long storeId, String publicId, String sourcePublicId);

    /**
     * Writes the same values onto many products at once, leaving their other characteristics alone.
     * Returns the number of products updated.
     */
    int applyAttributesBulk(Long storeId, List<String> publicIds, List<AttributeValue> values);

    /** The product's discount schedule, earliest period first, with statuses resolved now. */
    List<DiscountView> listDiscounts(Long storeId, String publicId);

    /**
     * Replaces the product's whole discount schedule. Entries carrying a public id keep their row and
     * creation time; a stored discount left out of the submission is removed.
     */
    List<DiscountView> replaceDiscounts(Long storeId, String publicId, List<SubmittedDiscount> discounts);

    /**
     * Applies one discount to many products, skipping those where it would collide with an existing
     * period or would not stay below the product's price.
     */
    DiscountBulkResultView applyDiscountsBulk(Long storeId, List<String> publicIds, Discount discount);
}
