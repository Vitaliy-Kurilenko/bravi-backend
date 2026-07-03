package ua.com.bravi.bravi.seller.catalog.products.api;

import ua.com.bravi.bravi.seller.catalog.products.domain.Product;
import ua.com.bravi.bravi.seller.catalog.products.domain.ProductSearchQuery;

import java.util.List;

public interface ProductsApi {

    ProductPage search(Long storeId, ProductSearchQuery query);

    ProductView getById(Long storeId, Long productId);

    Long create(Long storeId, Product product);

    void update(Long storeId, Long productId, Product patch);

    void delete(Long storeId, Long productId);

    List<ProductImageView> listImages(Long storeId, Long productId);

    ProductImageView addImage(Long storeId, Long productId, ImageUpload upload);

    ProductImageView replaceImage(Long storeId, Long productId, Long imageId, ImageUpload upload);

    void deleteImage(Long storeId, Long productId, Long imageId);

    ImageContent loadImageContent(Long storeId, Long productId, Long imageId);
}
