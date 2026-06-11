package ua.com.bravi.bravi.catalog.categories.api;

import ua.com.bravi.bravi.catalog.categories.domain.Category;

import java.util.List;

public interface CategoriesApi {

    List<CategoryView> findTreeByStoreId(Long storeId);

    CategoryView getById(Long storeId, Long categoryId);

    Long create(Long storeId, Category category);

    void update(Long storeId, Long categoryId, Category patch);

    void delete(Long storeId, Long categoryId);
}
