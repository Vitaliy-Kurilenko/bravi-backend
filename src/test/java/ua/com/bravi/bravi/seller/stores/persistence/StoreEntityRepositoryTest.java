package ua.com.bravi.bravi.seller.stores.persistence;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import ua.com.bravi.bravi.AbstractPostgresIT;
import ua.com.bravi.bravi.seller.stores.domain.StoreStatus;
import ua.com.bravi.bravi.seller.stores.domain.WorkingHours;
import ua.com.bravi.bravi.seller.stores.persistence.entity.StoreEntity;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Currency;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class StoreEntityRepositoryTest extends AbstractPostgresIT {

    @Autowired
    private IStoreEntityRepository storeRepository;

    @Autowired
    private EntityManager entityManager;

    /** Creates an ACCOUNT(SELLER) + its SELLER_ACCOUNT profile and returns the account id. */
    private Long persistSellerAccount() {
        Object accountId = entityManager.createNativeQuery(
                        "INSERT INTO accounts (public_id, type, status, created_at) " +
                                "VALUES (:pid, 'SELLER', 'ACTIVE', now()) RETURNING id")
                .setParameter("pid", UUID.randomUUID().toString())
                .getSingleResult();
        Long id = ((Number) accountId).longValue();
        entityManager.createNativeQuery(
                        "INSERT INTO seller_accounts (account_id, onboarding_status, created_at) " +
                                "VALUES (:aid, 'ACTIVE', now())")
                .setParameter("aid", id)
                .executeUpdate();
        return id;
    }

    private static StoreEntity newStore(Long sellerAccountId) {
        StoreEntity store = new StoreEntity();
        store.setPublicId(UUID.randomUUID().toString());
        store.setSellerAccountId(sellerAccountId);
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
        Long sellerAccountId = persistSellerAccount();

        StoreEntity saved = storeRepository.saveAndFlush(newStore(sellerAccountId));

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
        Long sellerAccountId = persistSellerAccount();

        StoreEntity saved = storeRepository.saveAndFlush(newStore(sellerAccountId));
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNull();

        saved.setName("Renamed");
        StoreEntity updated = storeRepository.saveAndFlush(saved);
        assertThat(updated.getUpdatedAt()).isNotNull();
    }

    @Test
    void allowsMultipleStoresPerAccount() {
        Long sellerAccountId = persistSellerAccount();
        storeRepository.saveAndFlush(newStore(sellerAccountId));
        storeRepository.saveAndFlush(newStore(sellerAccountId));

        assertThat(storeRepository.findAll()).hasSize(2);
    }

    @Test
    void findFirstBySellerAccountIdReturnsStore() {
        Long sellerAccountId = persistSellerAccount();
        storeRepository.saveAndFlush(newStore(sellerAccountId));

        assertThat(storeRepository.findFirstBySellerAccountIdOrderByIdAsc(sellerAccountId)).isPresent();
        assertThat(storeRepository.findFirstBySellerAccountIdOrderByIdAsc(sellerAccountId + 9999)).isEmpty();
    }
}
