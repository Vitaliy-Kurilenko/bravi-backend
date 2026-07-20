package ua.com.bravi.bravi.seller.stores.persistence.mapper;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ua.com.bravi.bravi.seller.stores.domain.Store;
import ua.com.bravi.bravi.seller.stores.domain.StoreStatus;
import ua.com.bravi.bravi.seller.stores.domain.WorkingHours;
import ua.com.bravi.bravi.seller.stores.api.StoreView;
import ua.com.bravi.bravi.seller.stores.persistence.entity.StoreEntity;
import ua.com.bravi.bravi.seller.stores.persistence.entity.StoreSettingsEntity;

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
        entity.setStatus(StoreStatus.ACTIVE);
        Instant created = Instant.parse("2026-01-01T00:00:00Z");
        entity.setCreatedAt(created);

        Store store = mapper.toDomain(entity);

        assertThat(store.id()).isEqualTo(5L);
        assertThat(store.sellerAccountId()).isEqualTo(77L);
        assertThat(store.name()).isEqualTo("Shop");
        // Settings-owned fields are not on the store row; they come from store_settings.
        assertThat(store.timezone()).isNull();
        assertThat(store.currency()).isNull();
        assertThat(store.allowReturn()).isNull();
        assertThat(store.workingHours()).isNull();
        assertThat(store.status()).isEqualTo(StoreStatus.ACTIVE);
        assertThat(store.createdAt()).isEqualTo(created);
    }

    @Test
    void toEntityIgnoresIdSellerStatusAndTimestamps() {
        Store store = new Store(
                99L, 77L, "Shop", "desc", "UA", "Kyiv obl.", "Kyiv",
                "01001", "Khreschatyk", null,
                KYIV, "https://logo", workingHours(), UAH, true,
                StoreStatus.DISABLED,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-02-01T00:00:00Z")
        );

        StoreEntity entity = mapper.toEntity(store);

        assertThat(entity.getId()).isNull();
        assertThat(entity.getSellerAccountId()).isNull();
        // status default from entity field initializer remains (ACTIVE), not the domain DISABLED
        assertThat(entity.getStatus()).isEqualTo(StoreStatus.ACTIVE);
        assertThat(entity.getCreatedAt()).isNull();
        assertThat(entity.getUpdatedAt()).isNull();

        assertThat(entity.getName()).isEqualTo("Shop");
        assertThat(entity.getCountry()).isEqualTo("UA");
    }

    @Test
    void updateEntityAppliesNonNullFieldsAndKeepsTheRest() {
        StoreEntity entity = new StoreEntity();
        entity.setName("Old");
        entity.setDescription("Old desc");
        entity.setCountry("UA");
        entity.setCity("Kyiv");
        entity.setStatus(StoreStatus.ACTIVE);

        Store patch = new Store(
                null, null, "New name", null, null, null, "Lviv",
                null, null, null, null, null, null, null, false,
                null, null, null
        );

        mapper.updateEntity(entity, patch);

        assertThat(entity.getName()).isEqualTo("New name");
        assertThat(entity.getCity()).isEqualTo("Lviv");

        // Unchanged fields are preserved
        assertThat(entity.getDescription()).isEqualTo("Old desc");
        assertThat(entity.getCountry()).isEqualTo("UA");
        assertThat(entity.getStatus()).isEqualTo(StoreStatus.ACTIVE);
    }

    @Test
    void toViewSourcesTimezoneAndCurrencyFromSettings() {
        StoreEntity entity = new StoreEntity();
        entity.setId(5L);
        entity.setName("Shop");
        entity.setStatus(StoreStatus.ACTIVE);
        Instant created = Instant.parse("2026-01-01T00:00:00Z");
        entity.setCreatedAt(created);

        StoreSettingsEntity settings = new StoreSettingsEntity();
        settings.setStoreId(5L);
        settings.setTimezone(KYIV);
        settings.setDefaultCurrency(UAH);
        settings.setAllowReturn(true);
        settings.setWorkingHours(workingHours());
        settings.setCreatedAt(Instant.parse("2020-01-01T00:00:00Z"));

        StoreView view = mapper.toView(entity, settings);

        assertThat(view.timezone()).isEqualTo(KYIV);
        assertThat(view.currency()).isEqualTo(UAH);
        assertThat(view.allowReturn()).isTrue();
        assertThat(view.workingHours().monday().from()).isEqualTo(LocalTime.of(9, 0));
        assertThat(view.status()).isEqualTo("ACTIVE");
        // Timestamps come from the store row, not the settings row.
        assertThat(view.createdAt()).isEqualTo(created);
    }

    @Test
    void updateSettingsAppliesStorePatchTimezoneAndCurrency() {
        StoreSettingsEntity settings = new StoreSettingsEntity();
        settings.setTimezone(ZoneId.of("UTC"));
        settings.setDefaultCurrency(Currency.getInstance("EUR"));
        settings.setDefaultLanguage(java.util.Locale.ENGLISH);
        settings.setAllowReturn(false);

        Store patch = new Store(
                null, null, null, null, null, null, null,
                null, null, null, KYIV, null, workingHours(), UAH, true,
                null, null, null
        );

        mapper.updateSettings(settings, patch);

        assertThat(settings.getTimezone()).isEqualTo(KYIV);
        assertThat(settings.getDefaultCurrency()).isEqualTo(UAH);
        assertThat(settings.getAllowReturn()).isTrue();
        assertThat(settings.getWorkingHours().monday().from()).isEqualTo(LocalTime.of(9, 0));
        // Settings-only fields are untouched by a store patch.
        assertThat(settings.getDefaultLanguage()).isEqualTo(java.util.Locale.ENGLISH);
    }

    private static WorkingHours workingHours() {
        WorkingHours.DayInterval interval =
                new WorkingHours.DayInterval(LocalTime.of(9, 0), LocalTime.of(18, 0), false);
        return new WorkingHours(interval, interval, interval, interval, interval, null, null);
    }
}
