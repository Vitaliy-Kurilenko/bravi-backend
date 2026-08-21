package ua.com.bravi.bravi.seller.tags.domain;

import org.junit.jupiter.api.Test;
import ua.com.bravi.bravi.seller.tags.exception.InvalidTagRequestException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TagTargetTest {

    @Test
    void readsTheUrlSegmentCaseInsensitively() {
        assertThat(TagTarget.fromPath("products")).isEqualTo(TagTarget.PRODUCT);
        assertThat(TagTarget.fromPath("Orders")).isEqualTo(TagTarget.ORDER);
        assertThat(TagTarget.fromPath("PRODUCT")).isEqualTo(TagTarget.PRODUCT);
    }

    @Test
    void anUnknownSegmentIsRejected() {
        assertThatThrownBy(() -> TagTarget.fromPath("customers"))
                .isInstanceOfSatisfying(InvalidTagRequestException.class,
                        ex -> assertThat(ex.getField()).isEqualTo("target"));
    }

    /** The method security expressions read resource(); a rename here would silently move the guard. */
    @Test
    void everyTargetNamesTheRbacResourceGuardingIt() {
        assertThat(TagTarget.PRODUCT.resource()).isEqualTo("PRODUCT");
        assertThat(TagTarget.ORDER.resource()).isEqualTo("ORDER");
    }
}
