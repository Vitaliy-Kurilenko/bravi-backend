package ua.com.bravi.bravi.seller.catalog.categories.domain;

import ua.com.bravi.bravi.seller.catalog.categories.exception.InvalidCategoryHierarchyException;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Invariants of a store category tree: a depth of at most {@link #MAX_DEPTH} levels and no cycles
 * when a node is moved. Pure functions over the flat set of categories of one store.
 */
public final class CategoryHierarchyPolicy {

    public static final int MAX_DEPTH = 3;

    private static final String FIELD_PARENT = "parent_public_id";

    private CategoryHierarchyPolicy() {
    }

    /** Validates creation of a new node under {@code parentId}, which must not be null. */
    public static void validateCreate(List<Category> storeCategories, Long parentId) {
        Map<Long, Category> byId = indexById(storeCategories);
        if (depthOf(parentId, byId) + 1 > MAX_DEPTH) {
            throw depthExceeded();
        }
    }

    /** Validates a move of node {@code nodeId} under {@code newParentId}, which must not be null. */
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

    /** Depth of a node, counted by walking up the parents; a root has depth 1. */
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

    /** Height of the subtree of a node; a leaf has height 1. */
    private static int heightOf(Long id, Map<Long, List<Category>> byParent) {
        int maxChild = 0;
        for (Category child : byParent.getOrDefault(id, List.of())) {
            maxChild = Math.max(maxChild, heightOf(child.id(), byParent));
        }
        return 1 + maxChild;
    }

    /** Tells whether {@code candidateId} is a descendant of {@code ancestorId}. */
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
