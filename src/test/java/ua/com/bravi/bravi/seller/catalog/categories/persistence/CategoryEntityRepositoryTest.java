package ua.com.bravi.bravi.seller.catalog.categories.persistence;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import ua.com.bravi.bravi.AbstractPostgresIT;
import ua.com.bravi.bravi.seller.catalog.categories.domain.CategoryStatus;
import ua.com.bravi.bravi.seller.catalog.categories.persistence.entity.CategoryEntity;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CategoryEntityRepositoryTest extends AbstractPostgresIT {

    @Autowired
    private ICategoryEntityRepository repository;

    @Autowired
    private EntityManager entityManager;

    private Long persistStore() {
        Object accountId = entityManager.createNativeQuery(
                        "INSERT INTO accounts (public_id, type, status, created_at) " +
                                "VALUES (:pid, 'SELLER', 'ACTIVE', now()) RETURNING id")
                .setParameter("pid", UUID.randomUUID().toString())
                .getSingleResult();
        long sellerAccountId = ((Number) accountId).longValue();
        entityManager.createNativeQuery(
                        "INSERT INTO seller_accounts (account_id, onboarding_status, created_at) " +
                                "VALUES (:aid, 'ACTIVE', now())")
                .setParameter("aid", sellerAccountId)
                .executeUpdate();

        Object storeId = entityManager.createNativeQuery(
                        "INSERT INTO stores (public_id, seller_account_id, name, status, created_at) " +
                                "VALUES (:spid, :sellerAccountId, 'Shop', 'ACTIVE', now()) RETURNING id")
                .setParameter("spid", UUID.randomUUID().toString())
                .setParameter("sellerAccountId", sellerAccountId)
                .getSingleResult();
        return ((Number) storeId).longValue();
    }

    private CategoryEntity newCategory(Long storeId, String name, Long parentId) {
        CategoryEntity entity = new CategoryEntity();
        entity.setPublicId("cat_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        entity.setStoreId(storeId);
        entity.setParentId(parentId);
        entity.setName(name);
        entity.setStatus(CategoryStatus.INACTIVE);
        return entity;
    }

    @Test
    void savesAndLoadsCategoryWithParentAndStatus() {
        Long storeId = persistStore();
        Long rootId = repository.saveAndFlush(newCategory(storeId, "Root", null)).getId();

        CategoryEntity child = repository.saveAndFlush(newCategory(storeId, "Child", rootId));
        entityManager.clear();

        CategoryEntity loaded = repository.findById(child.getId()).orElseThrow();
        assertThat(loaded.getParentId()).isEqualTo(rootId);
        assertThat(loaded.getStatus()).isEqualTo(CategoryStatus.INACTIVE);
        assertThat(loaded.getCreatedAt()).isNotNull();
    }

    @Test
    void enforcesUniqueRootName() {
        Long storeId = persistStore();
        repository.saveAndFlush(newCategory(storeId, "Electronics", null));

        assertThatThrownBy(() -> repository.saveAndFlush(newCategory(storeId, "Electronics", null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void allowsSameNameUnderDifferentParents() {
        Long storeId = persistStore();
        Long men = repository.saveAndFlush(newCategory(storeId, "Men", null)).getId();
        Long women = repository.saveAndFlush(newCategory(storeId, "Women", null)).getId();

        repository.saveAndFlush(newCategory(storeId, "Shoes", men));

        assertThatCode(() -> repository.saveAndFlush(newCategory(storeId, "Shoes", women)))
                .doesNotThrowAnyException();
    }

    @Test
    void existsByParentIdDetectsChildren() {
        Long storeId = persistStore();
        Long rootId = repository.saveAndFlush(newCategory(storeId, "Root", null)).getId();
        repository.saveAndFlush(newCategory(storeId, "Child", rootId));

        assertThat(repository.existsByParentId(rootId)).isTrue();
    }

    @Test
    void findByStoreIdAndPublicIdScopesToStore() {
        Long storeId = persistStore();
        Long otherStoreId = persistStore();
        CategoryEntity saved = repository.saveAndFlush(newCategory(storeId, "A", null));
        String publicId = saved.getPublicId();

        assertThat(repository.findByStoreIdAndPublicId(storeId, publicId)).isPresent();
        assertThat(repository.findByStoreIdAndPublicId(otherStoreId, publicId)).isEmpty();
    }

    @Test
    void findByStoreIdReturnsOnlyOwnStoreCategories() {
        Long storeId = persistStore();
        Long otherStoreId = persistStore();
        repository.saveAndFlush(newCategory(storeId, "A", null));
        repository.saveAndFlush(newCategory(storeId, "B", null));
        repository.saveAndFlush(newCategory(otherStoreId, "C", null));

        List<CategoryEntity> own = repository.findByStoreId(storeId);

        assertThat(own).extracting(CategoryEntity::getName).containsExactlyInAnyOrder("A", "B");
    }
}
