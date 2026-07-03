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
        Object sellerId = entityManager.createNativeQuery(
                        "INSERT INTO users (ext_id, type, first_name, email, status, created_at) " +
                                "VALUES (:extId, 'SELLER', 'John', :email, 'ACTIVE', now()) RETURNING id")
                .setParameter("extId", UUID.randomUUID())
                .setParameter("email", "seller-" + UUID.randomUUID() + "@example.com")
                .getSingleResult();

        Object storeId = entityManager.createNativeQuery(
                        "INSERT INTO stores (seller_id, name, timezone, currency, allow_return, status, created_at) " +
                                "VALUES (:sellerId, 'Shop', 'Europe/Kyiv', 'UAH', true, 'ACTIVE', now()) RETURNING id")
                .setParameter("sellerId", ((Number) sellerId).longValue())
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
