package ua.com.bravi.bravi.seller.controller.mapper;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ua.com.bravi.bravi.seller.controller.dto.in.StoreCreateRequest;
import ua.com.bravi.bravi.seller.controller.dto.in.StoreUpdateRequest;
import ua.com.bravi.bravi.seller.controller.dto.out.StoreResponse;
import ua.com.bravi.bravi.seller.stores.api.StoreView;
import ua.com.bravi.bravi.seller.stores.domain.Store;
import ua.com.bravi.bravi.seller.stores.domain.WorkingHours;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;

class StoreDtoMapperTest {

    private final StoreDtoMapper mapper = Mappers.getMapper(StoreDtoMapper.class);

    private static final ZoneId KYIV = ZoneId.of("Europe/Kyiv");
    private static final Currency UAH = Currency.getInstance("UAH");

    @Test
    void toResponseMapsAllFields() {
        WorkingHours wh = workingHours();
        Instant created = Instant.parse("2026-01-01T00:00:00Z");
        Instant updated = Instant.parse("2026-02-01T00:00:00Z");

        StoreView view = new StoreView(
                10L, 1L, "Shop", "Cool shop", "UA", "Kyiv obl.", "Kyiv",
                "01001", "Khreschatyk 1", "office 3",
                KYIV, "https://logo", wh, UAH, true,
                "ACTIVE", created, updated
        );

        StoreResponse response = mapper.toResponse(view);

        assertThat(response.name()).isEqualTo("Shop");
        assertThat(response.description()).isEqualTo("Cool shop");
        assertThat(response.country()).isEqualTo("UA");
        assertThat(response.region()).isEqualTo("Kyiv obl.");
        assertThat(response.city()).isEqualTo("Kyiv");
        assertThat(response.postalCode()).isEqualTo("01001");
        assertThat(response.address()).isEqualTo("Khreschatyk 1");
        assertThat(response.addressAdditional()).isEqualTo("office 3");
        assertThat(response.timezone()).isEqualTo(KYIV);
        assertThat(response.logoUrl()).isEqualTo("https://logo");
        assertThat(response.workingHours()).isEqualTo(wh);
        assertThat(response.currency()).isEqualTo(UAH);
        assertThat(response.allowReturn()).isTrue();
        assertThat(response.status()).isEqualTo("ACTIVE");
    }

    @Test
    void toDomainFromCreateRequestIgnoresServerSideFields() {
        StoreCreateRequest req = new StoreCreateRequest(
                "Shop", "desc", "UA", "Kyiv obl.", "Kyiv",
                "01001", "Address", "extra",
                KYIV, "https://logo", workingHours(), UAH, true
        );

        Store store = mapper.toDomain(req);

        assertThat(store.id()).isNull();
        assertThat(store.sellerId()).isNull();
        assertThat(store.status()).isNull();
        assertThat(store.createdAt()).isNull();
        assertThat(store.updatedAt()).isNull();

        assertThat(store.name()).isEqualTo("Shop");
        assertThat(store.country()).isEqualTo("UA");
        assertThat(store.postalCode()).isEqualTo("01001");
        assertThat(store.timezone()).isEqualTo(KYIV);
        assertThat(store.currency()).isEqualTo(UAH);
        assertThat(store.allowReturn()).isTrue();
    }

    @Test
    void toDomainFromUpdateRequestPropagatesNulls() {
        StoreUpdateRequest req = new StoreUpdateRequest(
                "NewName", null, null, null, null,
                null, null, null,
                null, null, null, null, null
        );

        Store store = mapper.toDomain(req);

        assertThat(store.name()).isEqualTo("NewName");
        assertThat(store.description()).isNull();
        assertThat(store.country()).isNull();
        assertThat(store.timezone()).isNull();
        assertThat(store.currency()).isNull();
        assertThat(store.allowReturn()).isNull();
        assertThat(store.id()).isNull();
        assertThat(store.sellerId()).isNull();
        assertThat(store.status()).isNull();
    }

    private static WorkingHours workingHours() {
        WorkingHours.DayInterval interval =
                new WorkingHours.DayInterval(LocalTime.of(9, 0), LocalTime.of(18, 0), false);
        return new WorkingHours(interval, interval, interval, interval, interval, null, null);
    }
}
