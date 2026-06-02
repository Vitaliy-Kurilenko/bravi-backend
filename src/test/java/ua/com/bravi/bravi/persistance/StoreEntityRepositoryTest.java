package ua.com.bravi.bravi.persistance;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import ua.com.bravi.bravi.AbstractPostgresIT;
import ua.com.bravi.bravi.domain.store.StoreStatus;
import ua.com.bravi.bravi.domain.store.WorkingHours;
import ua.com.bravi.bravi.domain.user.UserStatus;
import ua.com.bravi.bravi.domain.user.UserType;
import ua.com.bravi.bravi.persistance.entity.StoreEntity;
import ua.com.bravi.bravi.persistance.entity.UserEntity;

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
    private IUserEntityRepository userRepository;

    private UserEntity persistSeller() {
        UserEntity seller = new UserEntity();
        seller.setExtId(UUID.randomUUID());
        seller.setType(UserType.SELLER);
        seller.setFirstName("John");
        seller.setEmail("seller-" + UUID.randomUUID() + "@example.com");
        seller.setStatus(UserStatus.ACTIVE);
        return userRepository.saveAndFlush(seller);
    }

    private static StoreEntity newStore(UserEntity seller) {
        StoreEntity store = new StoreEntity();
        store.setSeller(seller);
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
        UserEntity seller = persistSeller();

        StoreEntity saved = storeRepository.saveAndFlush(newStore(seller));

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
        UserEntity seller = persistSeller();

        StoreEntity saved = storeRepository.saveAndFlush(newStore(seller));
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNull();

        saved.setName("Renamed");
        StoreEntity updated = storeRepository.saveAndFlush(saved);
        assertThat(updated.getUpdatedAt()).isNotNull();
    }

    @Test
    void enforcesUniqueSeller() {
        UserEntity seller = persistSeller();
        storeRepository.saveAndFlush(newStore(seller));

        assertThatThrownBy(() -> storeRepository.saveAndFlush(newStore(seller)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findBySellerIdReturnsStore() {
        UserEntity seller = persistSeller();
        storeRepository.saveAndFlush(newStore(seller));

        assertThat(storeRepository.findBySeller_Id(seller.getId())).isPresent();
        assertThat(storeRepository.findBySeller_Id(seller.getId() + 9999)).isEmpty();
    }

    @Test
    void existsBySellerIdReflectsPersistedState() {
        UserEntity seller = persistSeller();

        assertThat(storeRepository.existsBySeller_Id(seller.getId())).isFalse();
        storeRepository.saveAndFlush(newStore(seller));
        assertThat(storeRepository.existsBySeller_Id(seller.getId())).isTrue();
    }
}
