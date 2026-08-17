package ua.com.bravi.bravi.seller.catalog.attributes.domain;

import org.junit.jupiter.api.Test;
import ua.com.bravi.bravi.seller.catalog.attributes.domain.AttributeInheritance.Binding;
import ua.com.bravi.bravi.seller.catalog.attributes.domain.AttributeInheritance.EffectiveAttribute;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The category tree used here is {@code Clothing (1) → Outerwear (2) → Jackets (3)}, so a path is
 * written nearest-first, the way a product's category reports its ancestors.
 */
class AttributeInheritanceTest {

    private static final Attribute COLOR = attribute(100L, "COLOR", AttributeScope.CATEGORY);
    private static final Attribute MATERIAL = attribute(101L, "MATERIAL", AttributeScope.CATEGORY);
    private static final Attribute HOOD = attribute(102L, "HOOD", AttributeScope.CATEGORY);
    private static final Attribute WARRANTY = attribute(200L, "WARRANTY", AttributeScope.GLOBAL);

    private static final Map<Long, Attribute> BY_ID =
            Stream.of(COLOR, MATERIAL, HOOD, WARRANTY).collect(Collectors.toMap(Attribute::id, Function.identity()));

    @Test
    void aBindingOnAnAncestorReachesTheDescendant() {
        List<EffectiveAttribute> effective = AttributeInheritance.effective(
                List.of(3L, 2L, 1L), List.of(),
                List.of(new Binding(1L, COLOR.id(), 0)), BY_ID);

        assertThat(effective).singleElement().satisfies(entry -> {
            assertThat(entry.attribute()).isEqualTo(COLOR);
            assertThat(entry.source()).isEqualTo(AttributeSource.INHERITED);
            assertThat(entry.sourceCategoryId()).isEqualTo(1L);
        });
    }

    @Test
    void aBindingOnTheCategoryItselfIsReportedAsItsOwn() {
        List<EffectiveAttribute> effective = AttributeInheritance.effective(
                List.of(3L, 2L, 1L), List.of(),
                List.of(new Binding(3L, HOOD.id(), 0)), BY_ID);

        assertThat(effective).singleElement().satisfies(entry -> {
            assertThat(entry.source()).isEqualTo(AttributeSource.OWN);
            assertThat(entry.sourceCategoryId()).isEqualTo(3L);
        });
    }

    @Test
    void globalAttributesComeFirstAndNeedNoBinding() {
        List<EffectiveAttribute> effective = AttributeInheritance.effective(
                List.of(3L, 2L, 1L), List.of(WARRANTY),
                List.of(new Binding(1L, COLOR.id(), 0)), BY_ID);

        assertThat(effective).extracting(entry -> entry.attribute().code())
                .containsExactly("WARRANTY", "COLOR");
        assertThat(effective.getFirst().source()).isEqualTo(AttributeSource.GLOBAL);
        assertThat(effective.getFirst().sourceCategoryId()).isNull();
    }

    @Test
    void aGlobalAttributeAlsoReachesAProductWithoutACategory() {
        List<EffectiveAttribute> effective =
                AttributeInheritance.effective(List.of(), List.of(WARRANTY), List.of(), BY_ID);

        assertThat(effective).extracting(entry -> entry.attribute().code()).containsExactly("WARRANTY");
    }

    @Test
    void everyLevelOfTheTreeContributesInRootToLeafOrder() {
        List<EffectiveAttribute> effective = AttributeInheritance.effective(
                List.of(3L, 2L, 1L), List.of(),
                List.of(new Binding(3L, HOOD.id(), 0),
                        new Binding(1L, COLOR.id(), 0),
                        new Binding(2L, MATERIAL.id(), 0)), BY_ID);

        assertThat(effective).extracting(entry -> entry.attribute().code())
                .containsExactly("COLOR", "MATERIAL", "HOOD");
        assertThat(effective).extracting(EffectiveAttribute::source)
                .containsExactly(AttributeSource.INHERITED, AttributeSource.INHERITED, AttributeSource.OWN);
    }

    @Test
    void anAttributeBoundTwiceIsListedOnceWithTheSourceNearestTheProduct() {
        List<EffectiveAttribute> effective = AttributeInheritance.effective(
                List.of(3L, 2L, 1L), List.of(),
                List.of(new Binding(1L, COLOR.id(), 0), new Binding(3L, COLOR.id(), 0)), BY_ID);

        assertThat(effective).singleElement().satisfies(entry -> {
            assertThat(entry.source()).isEqualTo(AttributeSource.OWN);
            assertThat(entry.sourceCategoryId()).isEqualTo(3L);
        });
    }

    @Test
    void bindingsOfCategoriesOutsideThePathAreIgnored() {
        List<EffectiveAttribute> effective = AttributeInheritance.effective(
                List.of(3L, 2L, 1L), List.of(),
                List.of(new Binding(9L, MATERIAL.id(), 0)), BY_ID);

        assertThat(effective).isEmpty();
    }

    @Test
    void bindingsAreOrderedByTheirStoredPositionWithinACategory() {
        List<EffectiveAttribute> effective = AttributeInheritance.effective(
                List.of(1L), List.of(),
                List.of(new Binding(1L, MATERIAL.id(), 1), new Binding(1L, COLOR.id(), 0)), BY_ID);

        assertThat(effective).extracting(entry -> entry.attribute().code()).containsExactly("COLOR", "MATERIAL");
    }

    private static Attribute attribute(Long id, String code, AttributeScope scope) {
        return new Attribute(id, "attr_" + code, 1L, null, code, code, null, AttributeValueType.TEXT, scope,
                null, null, false, AttributeStatus.ACTIVE, null, null);
    }
}
