package ua.com.bravi.bravi.seller.stores.persistence;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import ua.com.bravi.bravi.AbstractPostgresIT;
import ua.com.bravi.bravi.seller.stores.domain.StoreStatus;
import ua.com.bravi.bravi.seller.stores.domain.WorkingHours;
import ua.com.bravi.bravi.seller.stores.persistence.entity.StoreEntity;
import ua.com.bravi.bravi.seller.stores.persistence.entity.StoreSettingsEntity;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Currency;
import java.util.Locale;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class StoreEntityRepositoryTest extends AbstractPostgresIT {

    @Autowired
    private IStoreEntityRepository storeRepository;

    @Autowired
    private IStoreSettingsRepository storeSettingsRepository;

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
                                "VALUES (:aid, 'NOT_STARTED', now())")
                .setParameter("aid", id)
                .executeUpdate();
        return id;
    }

    private static StoreEntity newStore(Long sellerAccountId) {
        StoreEntity store = new StoreEntity();
        store.setPublicId(UUID.randomUUID().toString());
        store.setSellerAccountId(sellerAccountId);
        store.setName("Shop");
        return store;
    }

    @Test
    void savesAndLoadsStore() {
        Long sellerAccountId = persistSellerAccount();

        StoreEntity saved = storeRepository.saveAndFlush(newStore(sellerAccountId));

        StoreEntity loaded = storeRepository.findById(saved.getId()).orElseThrow();
        assertThat(loaded.getStatus()).isEqualTo(StoreStatus.ACTIVE);
        assertThat(loaded.getName()).isEqualTo("Shop");
    }

    /** JSONB working hours and the ZoneId converter now round-trip through store_settings. */
    @Test
    void savesAndLoadsSettingsWithJsonAndZoneId() {
        Long sellerAccountId = persistSellerAccount();
        StoreEntity store = storeRepository.saveAndFlush(newStore(sellerAccountId));

        WorkingHours.DayInterval interval =
                new WorkingHours.DayInterval(LocalTime.of(9, 0), LocalTime.of(18, 0), false);
        StoreSettingsEntity settings = new StoreSettingsEntity();
        settings.setStoreId(store.getId());
        settings.setTimezone(ZoneId.of("Europe/Kyiv"));
        settings.setDefaultCurrency(Currency.getInstance("UAH"));
        settings.setDefaultLanguage(Locale.ENGLISH);
        settings.setDefaultWeightUnit("KG");
        settings.setDefaultDimensionUnit("CM");
        settings.setAllowReturn(true);
        settings.setWorkingHours(new WorkingHours(
                interval, interval, interval, interval, interval, null, null));
        storeSettingsRepository.saveAndFlush(settings);
        entityManager.clear();

        StoreSettingsEntity loaded = storeSettingsRepository.findById(store.getId()).orElseThrow();
        assertThat(loaded.getTimezone()).isEqualTo(ZoneId.of("Europe/Kyiv"));
        assertThat(loaded.getDefaultCurrency()).isEqualTo(Currency.getInstance("UAH"));
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
    void enforcesSingleStorePerAccount() {
        Long sellerAccountId = persistSellerAccount();
        storeRepository.saveAndFlush(newStore(sellerAccountId));

        assertThatThrownBy(() -> storeRepository.saveAndFlush(newStore(sellerAccountId)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findFirstBySellerAccountIdReturnsStore() {
        Long sellerAccountId = persistSellerAccount();
        storeRepository.saveAndFlush(newStore(sellerAccountId));

        assertThat(storeRepository.findFirstBySellerAccountIdOrderByIdAsc(sellerAccountId)).isPresent();
        assertThat(storeRepository.findFirstBySellerAccountIdOrderByIdAsc(sellerAccountId + 9999)).isEmpty();
    }
}
