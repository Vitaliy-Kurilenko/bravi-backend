package ua.com.bravi.bravi.seller.catalog.manufacturers.persistence;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import ua.com.bravi.bravi.AbstractPostgresIT;
import ua.com.bravi.bravi.seller.catalog.manufacturers.domain.ManufacturerStatus;
import ua.com.bravi.bravi.seller.catalog.manufacturers.persistence.entity.ManufacturerEntity;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ManufacturerEntityRepositoryTest extends AbstractPostgresIT {

    @Autowired
    private IManufacturerEntityRepository repository;

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

    private static ManufacturerEntity newManufacturer(Long storeId, String name) {
        ManufacturerEntity entity = new ManufacturerEntity();
        entity.setStoreId(storeId);
        entity.setName(name);
        entity.setDescription("desc");
        entity.setStatus(ManufacturerStatus.INACTIVE);
        return entity;
    }

    @Test
    void savesAndLoadsManufacturer() {
        Long storeId = persistStore();

        ManufacturerEntity saved = repository.saveAndFlush(newManufacturer(storeId, "ACME"));
        entityManager.clear();

        ManufacturerEntity loaded = repository.findById(saved.getId()).orElseThrow();
        assertThat(loaded.getName()).isEqualTo("ACME");
        assertThat(loaded.getStoreId()).isEqualTo(storeId);
        assertThat(loaded.getStatus()).isEqualTo(ManufacturerStatus.INACTIVE);
        assertThat(loaded.getCreatedAt()).isNotNull();
    }

    @Test
    void enforcesUniqueStoreAndName() {
        Long storeId = persistStore();
        repository.saveAndFlush(newManufacturer(storeId, "ACME"));

        assertThatThrownBy(() -> repository.saveAndFlush(newManufacturer(storeId, "ACME")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findByStoreIdReturnsOnlyOwnStoreManufacturers() {
        Long storeId = persistStore();
        Long otherStoreId = persistStore();
        repository.saveAndFlush(newManufacturer(storeId, "ACME"));
        repository.saveAndFlush(newManufacturer(storeId, "Globex"));
        repository.saveAndFlush(newManufacturer(otherStoreId, "Initech"));

        List<ManufacturerEntity> own = repository.findByStoreId(storeId);

        assertThat(own).extracting(ManufacturerEntity::getName)
                .containsExactlyInAnyOrder("ACME", "Globex");
    }
}
