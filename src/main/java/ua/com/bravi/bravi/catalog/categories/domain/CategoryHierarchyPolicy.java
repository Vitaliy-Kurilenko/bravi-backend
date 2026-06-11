package ua.com.bravi.bravi.catalog.categories.domain;

import ua.com.bravi.bravi.catalog.categories.exception.InvalidCategoryHierarchyException;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Інваріанти дерева категорій магазину: глибина не більша за {@link #MAX_DEPTH} рівнів
 * та відсутність циклів при переміщенні. Чисті функції над плоским набором категорій магазину.
 */
public final class CategoryHierarchyPolicy {

    public static final int MAX_DEPTH = 3;

    private static final String FIELD_PARENT = "parentId";

    private CategoryHierarchyPolicy() {
    }

    /** Створення нового вузла під {@code parentId} (parentId не null). */
    public static void validateCreate(List<Category> storeCategories, Long parentId) {
        Map<Long, Category> byId = indexById(storeCategories);
        if (depthOf(parentId, byId) + 1 > MAX_DEPTH) {
            throw depthExceeded();
        }
    }

    /** Переміщення вузла {@code nodeId} під {@code newParentId} (newParentId не null). */
    public static void validateMove(List<Category> storeCategories, Long nodeId, Long newParentId) {
        if (newParentId.equals(nodeId)) {
            throw new InvalidCategoryHierarchyException(FIELD_PARENT, "Category cannot be its own parent");
        }
        Map<Long, Category> byId = indexById(storeCategories);
        Map<Long, List<Category>> byParent = indexByParent(storeCategories);
        if (isDescendant(nodeId, newParentId, byParent)) {
            throw new InvalidCategoryHierarchyException(FIELD_PARENT,
                    "Category cannot be moved under its own descendant");
        }
        if (depthOf(newParentId, byId) + heightOf(nodeId, byParent) > MAX_DEPTH) {
            throw depthExceeded();
        }
    }

    /** Глибина вузла: корінь = 1, далі прогулянка вгору по parentId. */
    private static int depthOf(Long id, Map<Long, Category> byId) {
        int depth = 0;
        Long current = id;
        while (current != null && depth <= MAX_DEPTH) {
            Category node = byId.get(current);
            depth++;
            current = (node == null) ? null : node.parentId();
        }
        return depth;
    }

    /** Висота піддерева вузла: лист = 1. */
    private static int heightOf(Long id, Map<Long, List<Category>> byParent) {
        int maxChild = 0;
        for (Category child : byParent.getOrDefault(id, List.of())) {
            maxChild = Math.max(maxChild, heightOf(child.id(), byParent));
        }
        return 1 + maxChild;
    }

    /** Чи є {@code candidateId} нащадком {@code ancestorId}. */
    private static boolean isDescendant(Long ancestorId, Long candidateId, Map<Long, List<Category>> byParent) {
        for (Category child : byParent.getOrDefault(ancestorId, List.of())) {
            if (child.id().equals(candidateId) || isDescendant(child.id(), candidateId, byParent)) {
                return true;
            }
        }
        return false;
    }

    private static Map<Long, Category> indexById(List<Category> categories) {
        return categories.stream().collect(Collectors.toMap(Category::id, Function.identity()));
    }

    private static Map<Long, List<Category>> indexByParent(List<Category> categories) {
        return categories.stream()
                .filter(category -> category.parentId() != null)
                .collect(Collectors.groupingBy(Category::parentId));
    }

    private static InvalidCategoryHierarchyException depthExceeded() {
        return new InvalidCategoryHierarchyException(FIELD_PARENT,
                "Category tree depth must not exceed " + MAX_DEPTH + " levels");
    }
}
