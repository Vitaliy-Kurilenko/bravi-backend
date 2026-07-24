package ua.com.bravi.bravi.seller.catalog.products.persistence;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import ua.com.bravi.bravi.AbstractPostgresIT;
import ua.com.bravi.bravi.seller.catalog.products.domain.ProductSearchQuery;
import ua.com.bravi.bravi.seller.catalog.products.domain.ProductStatus;
import ua.com.bravi.bravi.seller.catalog.products.persistence.entity.ProductEntity;
import ua.com.bravi.bravi.shared.common.SortOrder;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProductEntityRepositoryTest extends AbstractPostgresIT {

    private static final Long IN_STOCK = 1L; // seeded by V9

    @Autowired
    private IProductEntityRepository repository;

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

    private ProductEntity newProduct(Long storeId, String name, String code, String price) {
        ProductEntity entity = new ProductEntity();
        entity.setPublicId("prd_" + UUID.randomUUID());
        entity.setStoreId(storeId);
        entity.setStockStatusId(IN_STOCK);
        entity.setName(name);
        entity.setCode(code);
        entity.setPrice(new BigDecimal(price));
        entity.setQuantity(1);
        entity.setStatus(ProductStatus.ACTIVE);
        return entity;
    }

    private static ProductSearchQuery query(String search, BigDecimal minPrice) {
        return new ProductSearchQuery(search, null, null, null, null, minPrice, null, null, null, null, null, 1, 20);
    }

    @Test
    void enforcesUniqueStoreCode() {
        Long storeId = persistStore();
        repository.saveAndFlush(newProduct(storeId, "A", "DUP", "10"));

        assertThatThrownBy(() -> repository.saveAndFlush(newProduct(storeId, "B", "DUP", "10")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void enforcesPartialUniqueSku() {
        Long storeId = persistStore();
        ProductEntity first = newProduct(storeId, "A", "C1", "10");
        first.setSku("SKU-1");
        repository.saveAndFlush(first);

        ProductEntity second = newProduct(storeId, "B", "C2", "10");
        second.setSku("SKU-1");
        assertThatThrownBy(() -> repository.saveAndFlush(second))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void searchMatchesNameCaseInsensitively() {
        Long storeId = persistStore();
        repository.saveAndFlush(newProduct(storeId, "Apple iPhone", "P1", "100"));
        repository.saveAndFlush(newProduct(storeId, "Samsung", "P2", "100"));

        Page<ProductEntity> result = repository.findAll(
                ProductSpecifications.forStore(storeId, query("apple", null), null, null), Pageable.unpaged());

        assertThat(result.getContent()).extracting(ProductEntity::getName).containsExactly("Apple iPhone");
    }

    @Test
    void filtersByMinPrice() {
        Long storeId = persistStore();
        repository.saveAndFlush(newProduct(storeId, "Cheap", "P1", "10"));
        repository.saveAndFlush(newProduct(storeId, "Pricey", "P2", "100"));

        Page<ProductEntity> result = repository.findAll(
                ProductSpecifications.forStore(storeId, query(null, new BigDecimal("50")), null, null), Pageable.unpaged());

        assertThat(result.getContent()).extracting(ProductEntity::getName).containsExactly("Pricey");
    }

    @Test
    void scopesAndPaginatesAndSorts() {
        Long storeId = persistStore();
        Long otherStore = persistStore();
        repository.saveAndFlush(newProduct(storeId, "C", "P1", "10"));
        repository.saveAndFlush(newProduct(storeId, "A", "P2", "10"));
        repository.saveAndFlush(newProduct(storeId, "B", "P3", "10"));
        repository.saveAndFlush(newProduct(otherStore, "Z", "P1", "10"));

        Pageable pageable = PageRequest.of(0, 2, Sort.by(Sort.Direction.ASC, "name"));
        Page<ProductEntity> firstPage = repository.findAll(
                ProductSpecifications.forStore(storeId, query(null, null), null, null), pageable);

        assertThat(firstPage.getTotalElements()).isEqualTo(3); // тільки свій магазин
        assertThat(firstPage.getContent()).extracting(ProductEntity::getName).containsExactly("A", "B");
    }

    @Test
    void filtersByStatus() {
        Long storeId = persistStore();
        ProductEntity active = newProduct(storeId, "Active", "P1", "10");
        ProductEntity inactive = newProduct(storeId, "Inactive", "P2", "10");
        inactive.setStatus(ProductStatus.INACTIVE);
        repository.saveAndFlush(active);
        repository.saveAndFlush(inactive);

        ProductSearchQuery q = new ProductSearchQuery(null, null, null, null,
                List.of(ProductStatus.INACTIVE), null, null, null, null, null, SortOrder.ASC, 1, 20);
        Page<ProductEntity> result = repository.findAll(
                ProductSpecifications.forStore(storeId, q, null, null), Pageable.unpaged());

        assertThat(result.getContent()).extracting(ProductEntity::getName).containsExactly("Inactive");
    }
}
