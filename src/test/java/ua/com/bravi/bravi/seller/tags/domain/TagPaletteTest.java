package ua.com.bravi.bravi.seller.tags.domain;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class TagPaletteTest {

    /** The choice must not drift between runs, or a tag would change colour on its own. */
    @Test
    void oneKeyAlwaysGivesTheSameColour() {
        assertThat(TagPalette.pick("хіт")).isEqualTo(TagPalette.pick("хіт"));
        assertThat(TagPalette.pick("розпродаж")).isEqualTo(TagPalette.pick("розпродаж"));
    }

    /** Nothing in the palette may be a value the colour rules would reject or rewrite. */
    @Test
    void everyColourIsAlreadyInTheStoredForm() {
        assertThat(TagPalette.colors()).allSatisfy(color ->
                assertThat(TagColor.normalize(color, "color")).isEqualTo(color));
    }

    @Test
    void aPickIsAlwaysOneOfThePaletteColours() {
        for (String key : List.of("хіт", "розпродаж", "новинка", "sale", "", "a")) {
            assertThat(TagPalette.colors()).contains(TagPalette.pick(key));
        }
    }

    @Test
    void differentNamesSpreadAcrossThePalette() {
        Set<String> picked = List.of("хіт", "розпродаж", "новинка", "терміново", "sale", "new",
                        "top", "gift", "b2b", "outlet", "sample", "preorder").stream()
                .map(TagPalette::pick)
                .collect(Collectors.toSet());

        assertThat(picked).hasSizeGreaterThan(1);
    }
}
