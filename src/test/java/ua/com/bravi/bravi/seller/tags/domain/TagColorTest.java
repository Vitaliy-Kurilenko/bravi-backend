package ua.com.bravi.bravi.seller.tags.domain;

import org.junit.jupiter.api.Test;
import ua.com.bravi.bravi.seller.tags.exception.InvalidTagRequestException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TagColorTest {

    private static final String FIELD = "color";

    @Test
    void theCanonicalFormIsUpperCase() {
        assertThat(TagColor.normalize("#e5484d", FIELD)).isEqualTo("#E5484D");
        assertThat(TagColor.normalize("#E5484D", FIELD)).isEqualTo("#E5484D");
    }

    @Test
    void surroundingSpaceIsTrimmed() {
        assertThat(TagColor.normalize("  #E5484D  ", FIELD)).isEqualTo("#E5484D");
    }

    @Test
    void theShorthandIsExpandedIntoTheStoredForm() {
        assertThat(TagColor.normalize("#f80", FIELD)).isEqualTo("#FF8800");
        assertThat(TagColor.normalize("#FFF", FIELD)).isEqualTo("#FFFFFF");
        assertThat(TagColor.normalize("#000", FIELD)).isEqualTo("#000000");
    }

    @Test
    void aMissingColourIsRejected() {
        for (String raw : new String[]{null, "", "   "}) {
            assertThatThrownBy(() -> TagColor.normalize(raw, FIELD))
                    .as("colour %s", raw)
                    .isInstanceOfSatisfying(InvalidTagRequestException.class,
                            ex -> assertThat(ex.getField()).isEqualTo(FIELD))
                    .hasMessage("Tag colour is required");
        }
    }

    @Test
    void anythingOutsideTheHexFormIsRejected() {
        for (String raw : new String[]{"red", "E5484D", "#12345", "#1234567", "#GGGGGG", "#E5484D;", "##FFF"}) {
            assertThatThrownBy(() -> TagColor.normalize(raw, FIELD))
                    .as("colour %s", raw)
                    .isInstanceOfSatisfying(InvalidTagRequestException.class,
                            ex -> assertThat(ex.getField()).isEqualTo(FIELD))
                    .hasMessageContaining("#E5484D");
        }
    }

    @Test
    void theCanonicalFormFitsTheColumn() {
        assertThat(TagColor.normalize("#abc", FIELD)).hasSize(TagColor.LENGTH);
    }
}
