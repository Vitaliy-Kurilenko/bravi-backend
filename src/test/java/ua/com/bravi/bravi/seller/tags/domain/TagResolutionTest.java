package ua.com.bravi.bravi.seller.tags.domain;

import org.junit.jupiter.api.Test;
import ua.com.bravi.bravi.seller.tags.exception.InvalidTagRequestException;
import ua.com.bravi.bravi.shared.exception.NotFoundException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TagResolutionTest {

    private static final Tag HIT = tag(1L, "tag_hit", "Хіт");
    private static final Tag SALE = tag(2L, "tag_sale", "Розпродаж");
    private static final List<Tag> STORE_TAGS = List.of(HIT, SALE);

    @Test
    void anExistingTagIsAddressedByItsPublicId() {
        TagResolution plan = TagResolution.plan(List.of(new TagRef("tag_hit", null)), STORE_TAGS);

        assertThat(plan.existing()).containsExactly(HIT);
        assertThat(plan.newNames()).isEmpty();
    }

    @Test
    void aNameMatchesAnExistingTagWhateverItsCaseOrSpacing() {
        TagResolution plan = TagResolution.plan(
                List.of(new TagRef(null, "  ХІТ  ")), STORE_TAGS);

        assertThat(plan.existing()).containsExactly(HIT);
        assertThat(plan.newNames()).isEmpty();
    }

    @Test
    void anUnknownNameIsMintedInTheSpellingThatWasTyped() {
        TagResolution plan = TagResolution.plan(
                List.of(new TagRef(null, "  Новинка  ")), STORE_TAGS);

        assertThat(plan.existing()).isEmpty();
        assertThat(plan.newNames()).containsExactly("Новинка");
    }

    @Test
    void caseVariantsOfOneNewNameCollapseIntoOneTag() {
        TagResolution plan = TagResolution.plan(
                List.of(new TagRef(null, "Новинка"), new TagRef(null, "новинка"),
                        new TagRef(null, "  НОВИНКА ")), STORE_TAGS);

        assertThat(plan.newNames()).containsExactly("Новинка");
    }

    @Test
    void anIdAndTheNameOfTheSameTagCollapse() {
        TagResolution plan = TagResolution.plan(
                List.of(new TagRef("tag_hit", null), new TagRef(null, "хіт")), STORE_TAGS);

        assertThat(plan.existing()).containsExactly(HIT);
        assertThat(plan.newNames()).isEmpty();
    }

    @Test
    void submittedOrderIsKept() {
        TagResolution plan = TagResolution.plan(
                List.of(new TagRef("tag_sale", null), new TagRef("tag_hit", null)), STORE_TAGS);

        assertThat(plan.existing()).containsExactly(SALE, HIT);
    }

    @Test
    void anIdTheStoreDoesNotOwnIsNotFound() {
        assertThatThrownBy(() -> TagResolution.plan(List.of(new TagRef("tag_other", null)), STORE_TAGS))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("tag_other");
    }

    @Test
    void anEntryWithNeitherIdNorNameAddressesItsOwnPosition() {
        assertThatThrownBy(() -> TagResolution.plan(
                List.of(new TagRef("tag_hit", null), new TagRef(null, null)), STORE_TAGS))
                .isInstanceOfSatisfying(InvalidTagRequestException.class,
                        ex -> assertThat(ex.getField()).isEqualTo("tags[1]"))
                .hasMessage("A tag id or a name is required");
    }

    @Test
    void aBlankNameIsRejectedRatherThanCreated() {
        assertThatThrownBy(() -> TagResolution.plan(List.of(new TagRef(null, "   ")), STORE_TAGS))
                .isInstanceOfSatisfying(InvalidTagRequestException.class,
                        ex -> assertThat(ex.getField()).isEqualTo("tags[0]"));
    }

    @Test
    void anOverlongNameAddressesTheEntryThatCarriesIt() {
        assertThatThrownBy(() -> TagResolution.plan(
                List.of(new TagRef("tag_hit", null), new TagRef(null, "a".repeat(65))), STORE_TAGS))
                .isInstanceOfSatisfying(InvalidTagRequestException.class,
                        ex -> assertThat(ex.getField()).isEqualTo("tags[1].name"));
    }

    @Test
    void anEmptySubmissionResolvesToNothing() {
        TagResolution plan = TagResolution.plan(List.of(), STORE_TAGS);

        assertThat(plan.existing()).isEmpty();
        assertThat(plan.newNames()).isEmpty();
    }

    private static Tag tag(Long id, String publicId, String name) {
        return Tag.builder().id(id).publicId(publicId).storeId(7L)
                .target(TagTarget.PRODUCT).name(name).status(TagStatus.ACTIVE).build();
    }
}
