package ua.com.bravi.bravi.seller.catalog.categories.api;

import ua.com.bravi.bravi.seller.catalog.categories.domain.Category;

import java.util.List;

public interface CategoriesApi {

    List<CategoryView> findTreeByStoreId(Long storeId);

    /** Internal lookup by bigint id, used by cross-module consumers such as products. */
    CategoryView getById(Long storeId, Long categoryId);

    CategoryView getByPublicId(Long storeId, String publicId);

    /**
     * The category and its ancestors, nearest first. This is the chain along which a category-bound
     * attribute is inherited; an unknown or null category yields an empty path.
     */
    List<CategoryPathEntry> findAncestorPath(Long storeId, Long categoryId);

    List<CategoryPathEntry> findAncestorPathByPublicId(Long storeId, String categoryPublicId);

    CategoryView create(Long storeId, Category category);

    void update(Long storeId, String publicId, Category patch);

    void delete(Long storeId, String publicId);
}
