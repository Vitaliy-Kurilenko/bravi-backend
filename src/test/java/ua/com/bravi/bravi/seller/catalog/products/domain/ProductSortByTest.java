package ua.com.bravi.bravi.seller.catalog.products.domain;

import org.junit.jupiter.api.Test;
import ua.com.bravi.bravi.seller.catalog.products.exception.InvalidProductRequestException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductSortByTest {

    @Test
    void fromParamIsCaseInsensitive() {
        assertThat(ProductSortBy.fromParam("created_at")).isEqualTo(ProductSortBy.CREATED_AT);
        assertThat(ProductSortBy.fromParam("PRICE")).isEqualTo(ProductSortBy.PRICE);
        assertThat(ProductSortBy.fromParam("name")).isEqualTo(ProductSortBy.NAME);
        assertThat(ProductSortBy.fromParam("id")).isEqualTo(ProductSortBy.ID);
    }

    @Test
    void fromParamRejectsUnknownField() {
        assertThatThrownBy(() -> ProductSortBy.fromParam("recommended_price"))
                .isInstanceOf(InvalidProductRequestException.class);
    }

    @Test
    void priceMapsToEntityFieldName() {
        assertThat(ProductSortBy.PRICE.getProperty()).isEqualTo("price");
    }
}
