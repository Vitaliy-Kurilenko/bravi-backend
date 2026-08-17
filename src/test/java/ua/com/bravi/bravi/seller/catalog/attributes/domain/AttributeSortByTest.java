package ua.com.bravi.bravi.seller.catalog.attributes.domain;

import org.junit.jupiter.api.Test;
import ua.com.bravi.bravi.seller.catalog.attributes.exception.InvalidAttributeRequestException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AttributeSortByTest {

    @Test
    void parsesExternalTokenCaseInsensitively() {
        assertThat(AttributeSortBy.fromParam("created_at")).isEqualTo(AttributeSortBy.CREATED_AT);
        assertThat(AttributeSortBy.fromParam("VALUE_TYPE")).isEqualTo(AttributeSortBy.VALUE_TYPE);
    }

    @Test
    void mapsToTheEntityPropertyUsedForSorting() {
        assertThat(AttributeSortBy.CREATED_AT.getProperty()).isEqualTo("createdAt");
        assertThat(AttributeSortBy.VALUE_TYPE.getProperty()).isEqualTo("valueType");
    }

    @Test
    void unknownTokenIsRejectedInsteadOfReachingTheQuery() {
        assertThatThrownBy(() -> AttributeSortBy.fromParam("drop table"))
                .isInstanceOf(InvalidAttributeRequestException.class)
                .hasMessage("Unknown sort field: drop table")
                .extracting("field").isEqualTo("sort_by");
    }
}
