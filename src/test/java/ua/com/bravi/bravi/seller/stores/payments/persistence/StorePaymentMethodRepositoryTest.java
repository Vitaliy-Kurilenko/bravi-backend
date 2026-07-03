package ua.com.bravi.bravi.seller.stores.payments.persistence;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import ua.com.bravi.bravi.AbstractPostgresIT;
import ua.com.bravi.bravi.seller.stores.payments.persistence.entity.StorePaymentMethodEntity;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class StorePaymentMethodRepositoryTest extends AbstractPostgresIT {

    @Autowired
    private IStorePaymentMethodRepository repository;

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

    private static StorePaymentMethodEntity newMethod(Long storeId, String code, boolean enabled) {
        StorePaymentMethodEntity entity = new StorePaymentMethodEntity();
        entity.setStoreId(storeId);
        entity.setMethodCode(code);
        entity.setEnabled(enabled);
        entity.setConfig(Map.of("secretKey", "sk_live_123", "publicKey", "pk_live_123"));
        return entity;
    }

    @Test
    void savesAndLoadsJsonbConfig() {
        Long storeId = persistStore();

        StorePaymentMethodEntity saved = repository.saveAndFlush(newMethod(storeId, "STRIPE", true));
        entityManager.clear();

        StorePaymentMethodEntity loaded = repository.findById(saved.getId()).orElseThrow();
        assertThat(loaded.getMethodCode()).isEqualTo("STRIPE");
        assertThat(loaded.getEnabled()).isTrue();
        assertThat(loaded.getConfig())
                .containsEntry("secretKey", "sk_live_123")
                .containsEntry("publicKey", "pk_live_123");
        assertThat(loaded.getCreatedAt()).isNotNull();
    }

    @Test
    void enforcesUniqueStoreAndMethodCode() {
        Long storeId = persistStore();
        repository.saveAndFlush(newMethod(storeId, "CASH_ON_DELIVERY", true));

        assertThatThrownBy(() -> repository.saveAndFlush(newMethod(storeId, "CASH_ON_DELIVERY", false)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findEnabledByStoreIdReturnsOnlyEnabled() {
        Long storeId = persistStore();
        repository.saveAndFlush(newMethod(storeId, "CASH_ON_DELIVERY", true));
        repository.saveAndFlush(newMethod(storeId, "STRIPE", false));

        List<StorePaymentMethodEntity> enabled = repository.findByStoreIdAndEnabledTrue(storeId);

        assertThat(enabled).singleElement()
                .satisfies(m -> assertThat(m.getMethodCode()).isEqualTo("CASH_ON_DELIVERY"));
    }
}
