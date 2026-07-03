package ua.com.bravi.bravi.seller.stores.persistence.mapper;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ua.com.bravi.bravi.seller.stores.domain.Store;
import ua.com.bravi.bravi.seller.stores.domain.StoreStatus;
import ua.com.bravi.bravi.seller.stores.domain.WorkingHours;
import ua.com.bravi.bravi.seller.stores.persistence.entity.StoreEntity;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;

class StoreEntityMapperTest {

    private final StoreEntityMapper mapper = Mappers.getMapper(StoreEntityMapper.class);

    private static final ZoneId KYIV = ZoneId.of("Europe/Kyiv");
    private static final Currency UAH = Currency.getInstance("UAH");

    @Test
    void toDomainMapsAllFieldsIncludingSellerId() {
        StoreEntity entity = new StoreEntity();
        entity.setId(5L);
        entity.setSellerAccountId(77L);
        entity.setName("Shop");
        entity.setDescription("Cool shop");
        entity.setCountry("UA");
        entity.setCity("Kyiv");
        entity.setTimezone(KYIV);
        entity.setCurrency(UAH);
        entity.setAllowReturn(true);
        entity.setStatus(StoreStatus.ACTIVE);
        entity.setWorkingHours(workingHours());
        Instant created = Instant.parse("2026-01-01T00:00:00Z");
        entity.setCreatedAt(created);

        Store store = mapper.toDomain(entity);

        assertThat(store.id()).isEqualTo(5L);
        assertThat(store.sellerAccountId()).isEqualTo(77L);
        assertThat(store.name()).isEqualTo("Shop");
        assertThat(store.timezone()).isEqualTo(KYIV);
        assertThat(store.currency()).isEqualTo(UAH);
        assertThat(store.allowReturn()).isTrue();
        assertThat(store.status()).isEqualTo(StoreStatus.ACTIVE);
        assertThat(store.createdAt()).isEqualTo(created);
    }

    @Test
    void toEntityIgnoresIdSellerStatusAndTimestamps() {
        Store store = new Store(
                99L, 77L, "Shop", "desc", "UA", "Kyiv obl.", "Kyiv",
                "01001", "Khreschatyk", null,
                KYIV, "https://logo", workingHours(), UAH, true,
                StoreStatus.BLOCKED,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-02-01T00:00:00Z")
        );

        StoreEntity entity = mapper.toEntity(store);

        assertThat(entity.getId()).isNull();
        assertThat(entity.getSellerAccountId()).isNull();
        // status default from entity field initializer remains (ACTIVE), not the domain BLOCKED
        assertThat(entity.getStatus()).isEqualTo(StoreStatus.ACTIVE);
        assertThat(entity.getCreatedAt()).isNull();
        assertThat(entity.getUpdatedAt()).isNull();

        assertThat(entity.getName()).isEqualTo("Shop");
        assertThat(entity.getCountry()).isEqualTo("UA");
        assertThat(entity.getTimezone()).isEqualTo(KYIV);
        assertThat(entity.getCurrency()).isEqualTo(UAH);
        assertThat(entity.getAllowReturn()).isTrue();
    }

    @Test
    void updateEntityAppliesNonNullFieldsAndKeepsTheRest() {
        StoreEntity entity = new StoreEntity();
        entity.setName("Old");
        entity.setDescription("Old desc");
        entity.setCountry("UA");
        entity.setCity("Kyiv");
        entity.setTimezone(KYIV);
        entity.setCurrency(UAH);
        entity.setAllowReturn(true);
        entity.setStatus(StoreStatus.ACTIVE);

        Store patch = new Store(
                null, null, "New name", null, null, null, "Lviv",
                null, null, null, null, null, null, null, false,
                null, null, null
        );

        mapper.updateEntity(entity, patch);

        assertThat(entity.getName()).isEqualTo("New name");
        assertThat(entity.getCity()).isEqualTo("Lviv");
        assertThat(entity.getAllowReturn()).isFalse();

        // Unchanged fields are preserved
        assertThat(entity.getDescription()).isEqualTo("Old desc");
        assertThat(entity.getCountry()).isEqualTo("UA");
        assertThat(entity.getTimezone()).isEqualTo(KYIV);
        assertThat(entity.getCurrency()).isEqualTo(UAH);
        assertThat(entity.getStatus()).isEqualTo(StoreStatus.ACTIVE);
    }

    private static WorkingHours workingHours() {
        WorkingHours.DayInterval interval =
                new WorkingHours.DayInterval(LocalTime.of(9, 0), LocalTime.of(18, 0), false);
        return new WorkingHours(interval, interval, interval, interval, interval, null, null);
    }
}
