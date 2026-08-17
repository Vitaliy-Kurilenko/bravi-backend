package ua.com.bravi.bravi.seller.catalog.attributes.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import ua.com.bravi.bravi.seller.catalog.attributes.exception.InvalidAttributeRequestException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AttributeOrderTest {

    private static final AttributeOrder OPTIONS = AttributeOrder.of(List.of(10L, 11L, 12L, 13L));

    @Test
    void moveToTheFrontShiftsTheOthersBack() {
        assertThat(OPTIONS.move(12L, 0)).containsExactly(12L, 10L, 11L, 13L);
    }

    @Test
    void moveToTheEndShiftsTheOthersForward() {
        assertThat(OPTIONS.move(10L, 3)).containsExactly(11L, 12L, 13L, 10L);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 4, 100})
    void moveOutsideTheListIsRejected(int target) {
        assertThatThrownBy(() -> OPTIONS.move(10L, target))
                .isInstanceOf(InvalidAttributeRequestException.class)
                .hasMessage("Position must be between 0 and 3")
                .extracting("field").isEqualTo("sort_order");
    }

    @Test
    void withoutClosesTheGap() {
        assertThat(OPTIONS.without(11L)).containsExactly(10L, 12L, 13L);
    }

    @Test
    void nextPositionAppendsAfterTheLastOption() {
        assertThat(OPTIONS.nextPosition()).isEqualTo(4);
        assertThat(AttributeOrder.of(List.of()).nextPosition()).isZero();
    }
}
