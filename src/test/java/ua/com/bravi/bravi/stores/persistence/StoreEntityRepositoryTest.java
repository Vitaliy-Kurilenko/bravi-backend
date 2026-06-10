package ua.com.bravi.bravi.stores.persistence;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import ua.com.bravi.bravi.AbstractPostgresIT;
import ua.com.bravi.bravi.stores.domain.StoreStatus;
import ua.com.bravi.bravi.stores.domain.WorkingHours;
import ua.com.bravi.bravi.stores.persistence.entity.StoreEntity;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Currency;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class StoreEntityRepositoryTest extends AbstractPostgresIT {

    @Autowired
    private IStoreEntityRepository storeRepository;

    @Autowired
    private EntityManager entityManager;

    private Long persistSeller() {
        Object id = entityManager.createNativeQuery(
                        "INSERT INTO users (ext_id, type, first_name, email, status, created_at) " +
                                "VALUES (:extId, 'SELLER', 'John', :email, 'ACTIVE', now()) RETURNING id")
                .setParameter("extId", UUID.randomUUID())
                .setParameter("email", "seller-" + UUID.randomUUID() + "@example.com")
                .getSingleResult();
        return ((Number) id).longValue();
    }

    private static StoreEntity newStore(Long sellerId) {
        StoreEntity store = new StoreEntity();
        store.setSellerId(sellerId);
        store.setName("Shop");
        store.setTimezone(ZoneId.of("Europe/Kyiv"));
        store.setCurrency(Currency.getInstance("UAH"));
        store.setAllowReturn(true);
        WorkingHours.DayInterval interval =
                new WorkingHours.DayInterval(LocalTime.of(9, 0), LocalTime.of(18, 0), false);
        store.setWorkingHours(new WorkingHours(
                interval, interval, interval, interval, interval, null, null));
        return store;
    }

    @Test
    void savesAndLoadsStoreWithJsonAndZoneId() {
        Long sellerId = persistSeller();

        StoreEntity saved = storeRepository.saveAndFlush(newStore(sellerId));

        StoreEntity loaded = storeRepository.findById(saved.getId()).orElseThrow();
        assertThat(loaded.getTimezone()).isEqualTo(ZoneId.of("Europe/Kyiv"));
        assertThat(loaded.getCurrency()).isEqualTo(Currency.getInstance("UAH"));
        assertThat(loaded.getStatus()).isEqualTo(StoreStatus.ACTIVE);
        assertThat(loaded.getAllowReturn()).isTrue();
        assertThat(loaded.getWorkingHours()).isNotNull();
        assertThat(loaded.getWorkingHours().monday().from()).isEqualTo(LocalTime.of(9, 0));
        assertThat(loaded.getWorkingHours().sunday()).isNull();
    }

    @Test
    void prePersistSetsCreatedAtAndPreUpdateSetsUpdatedAt() {
        Long sellerId = persistSeller();

        StoreEntity saved = storeRepository.saveAndFlush(newStore(sellerId));
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNull();

        saved.setName("Renamed");
        StoreEntity updated = storeRepository.saveAndFlush(saved);
        assertThat(updated.getUpdatedAt()).isNotNull();
    }

    @Test
    void enforcesUniqueSeller() {
        Long sellerId = persistSeller();
        storeRepository.saveAndFlush(newStore(sellerId));

        assertThatThrownBy(() -> storeRepository.saveAndFlush(newStore(sellerId)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findBySellerIdReturnsStore() {
        Long sellerId = persistSeller();
        storeRepository.saveAndFlush(newStore(sellerId));

        assertThat(storeRepository.findBySellerId(sellerId)).isPresent();
        assertThat(storeRepository.findBySellerId(sellerId + 9999)).isEmpty();
    }

    @Test
    void existsBySellerIdReflectsPersistedState() {
        Long sellerId = persistSeller();

        assertThat(storeRepository.existsBySellerId(sellerId)).isFalse();
        storeRepository.saveAndFlush(newStore(sellerId));
        assertThat(storeRepository.existsBySellerId(sellerId)).isTrue();
    }
}
