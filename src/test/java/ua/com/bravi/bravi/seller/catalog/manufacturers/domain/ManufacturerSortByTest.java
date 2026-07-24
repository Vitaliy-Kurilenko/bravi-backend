package ua.com.bravi.bravi.seller.catalog.manufacturers.domain;

import org.junit.jupiter.api.Test;
import ua.com.bravi.bravi.seller.catalog.manufacturers.exception.InvalidManufacturerRequestException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ManufacturerSortByTest {

    @Test
    void fromParamIsCaseInsensitive() {
        assertThat(ManufacturerSortBy.fromParam("created_at")).isEqualTo(ManufacturerSortBy.CREATED_AT);
        assertThat(ManufacturerSortBy.fromParam("NAME")).isEqualTo(ManufacturerSortBy.NAME);
        assertThat(ManufacturerSortBy.fromParam("status")).isEqualTo(ManufacturerSortBy.STATUS);
        assertThat(ManufacturerSortBy.fromParam("id")).isEqualTo(ManufacturerSortBy.ID);
    }

    @Test
    void fromParamRejectsUnknownField() {
        assertThatThrownBy(() -> ManufacturerSortBy.fromParam("description"))
                .isInstanceOf(InvalidManufacturerRequestException.class);
    }

    @Test
    void propertyMapsToEntityFieldName() {
        assertThat(ManufacturerSortBy.CREATED_AT.getProperty()).isEqualTo("createdAt");
    }
}
