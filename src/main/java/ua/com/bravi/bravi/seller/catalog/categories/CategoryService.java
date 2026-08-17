package ua.com.bravi.bravi.seller.catalog.categories;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.com.bravi.bravi.seller.catalog.categories.api.CategoriesApi;
import ua.com.bravi.bravi.seller.catalog.categories.api.CategoryPathEntry;
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
import ua.com.bravi.bravi.shared.util.PublicIdGenerator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService implements CategoriesApi {

    private static final String DUPLICATE_NAME = "Category with this name already exists under the same parent";

    private final ICategoryEntityRepository categoryRepository;
    private final CategoryEntityMapper categoryEntityMapper;

    @Override
    public List<CategoryView> findTreeByStoreId(Long storeId) {
        List<CategoryEntity> all = categoryRepository.findByStoreId(storeId);
        Map<Long, CategoryEntity> byId = indexById(all);
        Map<Long, List<CategoryEntity>> byParent = childrenByParent(all);
        return all.stream()
                .filter(entity -> entity.getParentId() == null)
                .map(root -> toTree(root, byId, byParent))
                .toList();
    }

    @Override
    public CategoryView getById(Long storeId, Long categoryId) {
        CategoryEntity node = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Category not found"));
        node.requireOwnedBy(storeId);
        return toSubtree(storeId, node);
    }

    @Override
    public CategoryView getByPublicId(Long storeId, String publicId) {
        return toSubtree(storeId, requireOwned(storeId, publicId));
    }

    @Override
    public List<CategoryPathEntry> findAncestorPath(Long storeId, Long categoryId) {
        if (categoryId == null) {
            return List.of();
        }
        return walkUp(indexById(categoryRepository.findByStoreId(storeId)), categoryId);
    }

    @Override
    public List<CategoryPathEntry> findAncestorPathByPublicId(Long storeId, String categoryPublicId) {
        List<CategoryEntity> all = categoryRepository.findByStoreId(storeId);
        Long categoryId = all.stream()
                .filter(category -> category.getPublicId().equals(categoryPublicId))
                .map(CategoryEntity::getId)
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Category not found"));
        return walkUp(indexById(all), categoryId);
    }

    @Override
    @Transactional
    public CategoryView create(Long storeId, Category category) {
        Long parentId = null;
        if (category.parentPublicId() != null) {
            List<CategoryEntity> storeCategories = categoryRepository.findByStoreId(storeId);
            parentId = resolveParentId(storeCategories, category.parentPublicId());
            CategoryHierarchyPolicy.validateCreate(categoryEntityMapper.toDomain(storeCategories), parentId);
        }
        CategoryEntity entity = categoryEntityMapper.toEntity(category);
        entity.setStoreId(storeId);
        entity.setParentId(parentId);
        entity.setPublicId(PublicIdGenerator.generate(PublicIdGenerator.CATEGORY_PREFIX));
        if (entity.getStatus() == null) {
            entity.setStatus(CategoryStatus.ACTIVE);
        }
        try {
            CategoryEntity saved = categoryRepository.save(entity);
            log.info("Category created storeId={} categoryId={} publicId={} parentId={}",
                    storeId, saved.getId(), saved.getPublicId(), parentId);
            return categoryEntityMapper.toView(saved, category.parentPublicId(), List.of());
        } catch (DataIntegrityViolationException duplicateName) {
            throw new CategoryAlreadyExistsException(DUPLICATE_NAME);
        }
    }

    @Override
    @Transactional
    public void update(Long storeId, String publicId, Category patch) {
        CategoryEntity entity = requireOwned(storeId, publicId);
        if (patch.parentPublicId() != null) {
            List<CategoryEntity> storeCategories = categoryRepository.findByStoreId(storeId);
            Long parentId = resolveParentId(storeCategories, patch.parentPublicId());
            CategoryHierarchyPolicy.validateMove(categoryEntityMapper.toDomain(storeCategories), entity.getId(), parentId);
            entity.setParentId(parentId);
        }
        categoryEntityMapper.updateEntity(entity, patch);
        try {
            categoryRepository.flush();
        } catch (DataIntegrityViolationException duplicateName) {
            throw new CategoryAlreadyExistsException(DUPLICATE_NAME);
        }
        log.info("Category updated storeId={} publicId={}", storeId, publicId);
    }

    @Override
    @Transactional
    public void delete(Long storeId, String publicId) {
        CategoryEntity entity = requireOwned(storeId, publicId);
        if (categoryRepository.existsByParentId(entity.getId())) {
            throw new CategoryHasChildrenException("Category has subcategories and cannot be deleted");
        }
        categoryRepository.delete(entity);
        log.info("Category deleted storeId={} publicId={}", storeId, publicId);
    }

    private CategoryView toSubtree(Long storeId, CategoryEntity node) {
        List<CategoryEntity> all = categoryRepository.findByStoreId(storeId);
        return toTree(node, indexById(all), childrenByParent(all));
    }

    private CategoryView toTree(CategoryEntity node, Map<Long, CategoryEntity> byId,
                                Map<Long, List<CategoryEntity>> byParent) {
        List<CategoryView> children = byParent.getOrDefault(node.getId(), List.of()).stream()
                .map(child -> toTree(child, byId, byParent))
                .toList();
        String parentPublicId = node.getParentId() == null ? null
                : Optional.ofNullable(byId.get(node.getParentId()))
                        .map(CategoryEntity::getPublicId)
                        .orElse(null);
        return categoryEntityMapper.toView(node, parentPublicId, children);
    }

    /** Walks parent links from the node up to its root, guarding against a cycle in stored data. */
    private static List<CategoryPathEntry> walkUp(Map<Long, CategoryEntity> byId, Long categoryId) {
        List<CategoryPathEntry> path = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        for (CategoryEntity node = byId.get(categoryId); node != null && visited.add(node.getId());
             node = node.getParentId() == null ? null : byId.get(node.getParentId())) {
            path.add(new CategoryPathEntry(node.getId(), node.getPublicId(), node.getName()));
        }
        return List.copyOf(path);
    }

    private static Map<Long, CategoryEntity> indexById(List<CategoryEntity> all) {
        return all.stream().collect(Collectors.toMap(CategoryEntity::getId, Function.identity()));
    }

    private static Map<Long, List<CategoryEntity>> childrenByParent(List<CategoryEntity> all) {
        return all.stream()
                .filter(entity -> entity.getParentId() != null)
                .collect(Collectors.groupingBy(CategoryEntity::getParentId));
    }

    private Long resolveParentId(List<CategoryEntity> storeCategories, String parentPublicId) {
        return storeCategories.stream()
                .filter(category -> parentPublicId.equals(category.getPublicId()))
                .map(CategoryEntity::getId)
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Parent category not found"));
    }

    private CategoryEntity requireOwned(Long storeId, String publicId) {
        return categoryRepository.findByStoreIdAndPublicId(storeId, publicId)
                .orElseThrow(() -> new NotFoundException("Category not found"));
    }
}
