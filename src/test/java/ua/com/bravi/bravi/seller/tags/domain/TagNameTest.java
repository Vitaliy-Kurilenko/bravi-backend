package ua.com.bravi.bravi.seller.tags.domain;

import org.junit.jupiter.api.Test;
import ua.com.bravi.bravi.seller.tags.exception.InvalidTagRequestException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TagNameTest {

    private static final String FIELD = "tags[0].name";

    @Test
    void trimsAndCollapsesInternalWhitespace() {
        assertThat(TagName.normalize("  хіт   продажів ", FIELD)).isEqualTo("хіт продажів");
        assertThat(TagName.normalize("Хіт\t\nсезону", FIELD)).isEqualTo("Хіт сезону");
    }

    @Test
    void keepsTheSpellingTheSellerTyped() {
        assertThat(TagName.normalize("ХІТ", FIELD)).isEqualTo("ХІТ");
    }

    @Test
    void blankNamesAreRejectedAgainstTheGivenField() {
        for (String raw : new String[]{null, "", "   ", "\t"}) {
            assertThatThrownBy(() -> TagName.normalize(raw, FIELD))
                    .as("name %s", raw)
                    .isInstanceOfSatisfying(InvalidTagRequestException.class,
                            ex -> assertThat(ex.getField()).isEqualTo(FIELD))
                    .hasMessage("Tag name is required");
        }
    }

    @Test
    void theLengthLimitCountsTheNormalizedForm() {
        String longest = "a".repeat(TagName.MAX_LENGTH);
        assertThat(TagName.normalize(longest, FIELD)).hasSize(TagName.MAX_LENGTH);
        assertThat(TagName.normalize("  " + longest + "  ", FIELD)).hasSize(TagName.MAX_LENGTH);

        assertThatThrownBy(() -> TagName.normalize("a".repeat(TagName.MAX_LENGTH + 1), FIELD))
                .isInstanceOf(InvalidTagRequestException.class)
                .hasMessage("Tag name must not exceed 64 characters");
    }

    @Test
    void theKeyFoldsCaseInBothAlphabets() {
        assertThat(TagName.key("Хіт")).isEqualTo(TagName.key("ХІТ"));
        assertThat(TagName.key("SALE")).isEqualTo("sale");
    }

    @Test
    void errorFieldsAddressTheSubmittedEntry() {
        assertThat(TagName.fieldOf(2, "name")).isEqualTo("tags[2].name");
    }
}
