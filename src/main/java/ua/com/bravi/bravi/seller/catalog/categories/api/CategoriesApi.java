package ua.com.bravi.bravi.seller.catalog.categories.api;

import ua.com.bravi.bravi.seller.catalog.categories.domain.Category;

import java.util.List;

public interface CategoriesApi {

    List<CategoryView> findTreeByStoreId(Long storeId);

    /** Internal lookup by bigint id, used by cross-module consumers such as products. */
    CategoryView getById(Long storeId, Long categoryId);

    CategoryView getByPublicId(Long storeId, String publicId);

    CategoryView create(Long storeId, Category category);

    void update(Long storeId, String publicId, Category patch);

    void delete(Long storeId, String publicId);
}
