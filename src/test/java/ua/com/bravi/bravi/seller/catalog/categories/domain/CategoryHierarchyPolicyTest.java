package ua.com.bravi.bravi.seller.catalog.categories.domain;

import org.junit.jupiter.api.Test;
import ua.com.bravi.bravi.seller.catalog.categories.exception.InvalidCategoryHierarchyException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CategoryHierarchyPolicyTest {

    // Store tree:
    //   1 (root, lvl1) -> 2 (lvl2) -> 3 (lvl3)
    //   10 (root, lvl1) -> 11 (lvl2)
    private static final List<Category> TREE = List.of(
            cat(1L, null),
            cat(2L, 1L),
            cat(3L, 2L),
            cat(10L, null),
            cat(11L, 10L)
    );

    private static Category cat(Long id, Long parentId) {
        return new Category(id, null, 7L, parentId, null, "c" + id, null, CategoryStatus.ACTIVE, null, null);
    }

    @Test
    void createUnderRootIsAllowed() {
        assertThatCode(() -> CategoryHierarchyPolicy.validateCreate(TREE, 1L)).doesNotThrowAnyException();
    }

    @Test
    void createUnderSecondLevelIsAllowed() {
        assertThatCode(() -> CategoryHierarchyPolicy.validateCreate(TREE, 2L)).doesNotThrowAnyException();
    }

    @Test
    void createUnderThirdLevelExceedsDepth() {
        assertThatThrownBy(() -> CategoryHierarchyPolicy.validateCreate(TREE, 3L))
                .isInstanceOf(InvalidCategoryHierarchyException.class);
    }

    @Test
    void moveUnderOwnDescendantIsCycle() {
        assertThatThrownBy(() -> CategoryHierarchyPolicy.validateMove(TREE, 2L, 3L))
                .isInstanceOf(InvalidCategoryHierarchyException.class);
    }

    @Test
    void moveUnderSelfIsRejected() {
        assertThatThrownBy(() -> CategoryHierarchyPolicy.validateMove(TREE, 2L, 2L))
                .isInstanceOf(InvalidCategoryHierarchyException.class);
    }

    @Test
    void moveExceedingDepthIsRejected() {
        // Node 2 has a subtree of height 2 (2->3); under node 11 (lvl2) that gives 2+2 = 4 > 3
        assertThatThrownBy(() -> CategoryHierarchyPolicy.validateMove(TREE, 2L, 11L))
                .isInstanceOf(InvalidCategoryHierarchyException.class);
    }

    @Test
    void validMoveOfLeafIsAllowed() {
        // Leaf 3 (height 1) under root 10 (lvl1) gives 1+1 = 2
        assertThatCode(() -> CategoryHierarchyPolicy.validateMove(TREE, 3L, 10L)).doesNotThrowAnyException();
    }
}
