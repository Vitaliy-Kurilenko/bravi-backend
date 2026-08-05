package ua.com.bravi.bravi.seller.catalog.products.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import ua.com.bravi.bravi.seller.catalog.products.exception.InvalidProductRequestException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductGalleryTest {

    private static final ProductGallery GALLERY = ProductGallery.of(List.of(10L, 11L, 12L, 13L));

    @Test
    void moveToTheFrontShiftsTheOthersBack() {
        assertThat(GALLERY.move(12L, 0)).containsExactly(12L, 10L, 11L, 13L);
    }

    @Test
    void moveToTheBackShiftsTheOthersForward() {
        assertThat(GALLERY.move(10L, 3)).containsExactly(11L, 12L, 13L, 10L);
    }

    @Test
    void moveToTheCurrentPositionKeepsTheOrder() {
        assertThat(GALLERY.move(11L, 1)).containsExactly(10L, 11L, 12L, 13L);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 4, 100})
    void moveOutsideTheGalleryIsRejected(int target) {
        assertThatThrownBy(() -> GALLERY.move(10L, target))
                .isInstanceOf(InvalidProductRequestException.class)
                .hasMessage("Position must be between 0 and 3")
                .extracting("field").isEqualTo("sort_order");
    }

    @Test
    void withoutDropsTheImageAndKeepsTheOrder() {
        assertThat(GALLERY.without(11L)).containsExactly(10L, 12L, 13L);
    }

    @Test
    void withoutAnUnknownImageKeepsTheGallery() {
        assertThat(GALLERY.without(99L)).containsExactly(10L, 11L, 12L, 13L);
    }

    @Test
    void nextPositionIsTheEndOfTheGallery() {
        assertThat(GALLERY.nextPosition()).isEqualTo(4);
        assertThat(ProductGallery.of(List.of()).nextPosition()).isZero();
    }

    @Test
    void onlyTheFirstPositionIsPrimary() {
        assertThat(ProductGallery.isPrimary(0)).isTrue();
        assertThat(ProductGallery.isPrimary(1)).isFalse();
        assertThat(ProductGallery.isPrimary(null)).isFalse();
    }
}
