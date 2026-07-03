package ua.com.bravi.bravi.seller.catalog.categories;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.com.bravi.bravi.seller.catalog.categories.api.CategoriesApi;
import ua.com.bravi.bravi.seller.catalog.categories.api.CategoryView;
import ua.com.bravi.bravi.seller.catalog.categories.domain.Category;
import ua.com.bravi.bravi.seller.catalog.categories.domain.CategoryHierarchyPolicy;
import ua.com.bravi.bravi.seller.catalog.categories.domain.CategoryStatus;
import ua.com.bravi.bravi.seller.catalog.categories.exception.CategoryAlreadyExistsException;
import ua.com.bravi.bravi.seller.catalog.categories.exception.CategoryHasChildrenException;
import ua.com.bravi.bravi.seller.catalog.categories.persistence.ICategoryEntityRepository;
import ua.com.bravi.bravi.seller.catalog.categories.persistence.entity.CategoryEntity;
import ua.com.bravi.bravi.seller.catalog.categories.persistence.mapper.CategoryEntityMapper;
import ua.com.bravi.bravi.shared.exception.NotFoundException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService implements CategoriesApi {

    private static final String DUPLICATE_NAME = "Category with this name already exists under the same parent";

    private final ICategoryEntityRepository categoryRepository;
    private final CategoryEntityMapper categoryEntityMapper;

    @Override
    public List<CategoryView> findTreeByStoreId(Long storeId) {
        List<CategoryEntity> all = categoryRepository.findByStoreId(storeId);
        Map<Long, List<CategoryEntity>> byParent = childrenByParent(all);
        return all.stream()
                .filter(entity -> entity.getParentId() == null)
                .map(root -> toTree(root, byParent))
                .toList();
    }

    @Override
    public CategoryView getById(Long storeId, Long categoryId) {
        CategoryEntity node = requireOwned(storeId, categoryId);
        Map<Long, List<CategoryEntity>> byParent = childrenByParent(categoryRepository.findByStoreId(storeId));
        return toTree(node, byParent);
    }

    @Override
    @Transactional
    public Long create(Long storeId, Category category) {
        if (category.parentId() != null) {
            List<Category> storeCategories = categoryEntityMapper.toDomain(categoryRepository.findByStoreId(storeId));
            requireParentPresent(storeCategories, category.parentId());
            CategoryHierarchyPolicy.validateCreate(storeCategories, category.parentId());
        }
        CategoryEntity entity = categoryEntityMapper.toEntity(category);
        entity.setStoreId(storeId);
        if (entity.getStatus() == null) {
            entity.setStatus(CategoryStatus.ACTIVE);
        }
        try {
            return categoryRepository.save(entity).getId();
        } catch (DataIntegrityViolationException duplicateName) {
            throw new CategoryAlreadyExistsException(DUPLICATE_NAME);
        }
    }

    @Override
    @Transactional
    public void update(Long storeId, Long categoryId, Category patch) {
        CategoryEntity entity = requireOwned(storeId, categoryId);
        if (patch.parentId() != null) {
            List<Category> storeCategories = categoryEntityMapper.toDomain(categoryRepository.findByStoreId(storeId));
            requireParentPresent(storeCategories, patch.parentId());
            CategoryHierarchyPolicy.validateMove(storeCategories, categoryId, patch.parentId());
            entity.setParentId(patch.parentId());
        }
        categoryEntityMapper.updateEntity(entity, patch);
        try {
            categoryRepository.flush();
        } catch (DataIntegrityViolationException duplicateName) {
            throw new CategoryAlreadyExistsException(DUPLICATE_NAME);
        }
    }

    @Override
    @Transactional
    public void delete(Long storeId, Long categoryId) {
        CategoryEntity entity = requireOwned(storeId, categoryId);
        if (categoryRepository.existsByParentId(categoryId)) {
            throw new CategoryHasChildrenException("Category has subcategories and cannot be deleted");
        }
        categoryRepository.delete(entity);
    }

    private CategoryView toTree(CategoryEntity node, Map<Long, List<CategoryEntity>> byParent) {
        List<CategoryView> children = byParent.getOrDefault(node.getId(), List.of()).stream()
                .map(child -> toTree(child, byParent))
                .toList();
        return categoryEntityMapper.toView(node, children);
    }

    private static Map<Long, List<CategoryEntity>> childrenByParent(List<CategoryEntity> all) {
        return all.stream()
                .filter(entity -> entity.getParentId() != null)
                .collect(Collectors.groupingBy(CategoryEntity::getParentId));
    }

    private void requireParentPresent(List<Category> storeCategories, Long parentId) {
        boolean present = storeCategories.stream().anyMatch(category -> category.id().equals(parentId));
        if (!present) {
            throw new NotFoundException("Parent category not found");
        }
    }

    private CategoryEntity requireOwned(Long storeId, Long categoryId) {
        CategoryEntity entity = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Category not found"));
        entity.requireOwnedBy(storeId);
        return entity;
    }
}
