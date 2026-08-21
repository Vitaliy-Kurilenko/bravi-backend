package ua.com.bravi.bravi.seller.tags.domain;

import org.junit.jupiter.api.Test;
import ua.com.bravi.bravi.seller.tags.exception.InvalidTagRequestException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TagSortByTest {

    @Test
    void readsTheExternalTokenCaseInsensitively() {
        assertThat(TagSortBy.fromParam("created_at")).isEqualTo(TagSortBy.CREATED_AT);
        assertThat(TagSortBy.fromParam("NAME")).isEqualTo(TagSortBy.NAME);
    }

    /** The property name is what builds the Sort, so no client string reaches the query. */
    @Test
    void everyFieldNamesAnEntityProperty() {
        assertThat(TagSortBy.NAME.getProperty()).isEqualTo("name");
        assertThat(TagSortBy.CREATED_AT.getProperty()).isEqualTo("createdAt");
        assertThat(TagSortBy.UPDATED_AT.getProperty()).isEqualTo("updatedAt");
    }

    @Test
    void anUnknownTokenIsRejectedAgainstTheSortField() {
        assertThatThrownBy(() -> TagSortBy.fromParam("price"))
                .isInstanceOfSatisfying(InvalidTagRequestException.class,
                        ex -> assertThat(ex.getField()).isEqualTo("sort_by"));
    }
}
