package ua.com.bravi.bravi.seller.catalog.attributes.domain;

import org.junit.jupiter.api.Test;
import ua.com.bravi.bravi.seller.catalog.attributes.exception.InvalidAttributeRequestException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AttributeValuePolicyTest {

    private static final Set<String> OPTIONS = Set.of("aopt_red", "aopt_blue");

    @Test
    void textValueIsAccepted() {
        assertThatCode(() -> AttributeValuePolicy.validate(
                definition(AttributeValueType.TEXT, null), Set.of(), text("Cotton lining")))
                .doesNotThrowAnyException();
    }

    @Test
    void blankTextIsRejected() {
        assertRejected(definition(AttributeValueType.TEXT, null), Set.of(), text("  "),
                "A text value is required");
    }

    @Test
    void overlongTextIsRejected() {
        String tooLong = "x".repeat(AttributeValuePolicy.MAX_TEXT_LENGTH + 1);
        assertRejected(definition(AttributeValueType.TEXT, null), Set.of(), text(tooLong),
                "Text value must not exceed 4000 characters");
    }

    @Test
    void numberWithoutAValueIsRejected() {
        assertRejected(definition(AttributeValueType.NUMBER, null), Set.of(), empty(),
                "A numeric value is required");
    }

    @Test
    void numberOfAUnitBearingAttributeNeedsAUnit() {
        AttributeValue value = new AttributeValue("attr_x", null, BigDecimal.TEN, null, null, null, null);
        assertRejected(definition(AttributeValueType.NUMBER, "WEIGHT_UNIT"), Set.of(), value,
                "A unit from the 'WEIGHT_UNIT' dictionary is required");
    }

    @Test
    void unitOnAnAttributeThatHasNoDictionaryIsRejected() {
        AttributeValue value = new AttributeValue("attr_x", null, BigDecimal.TEN, null, null, "KG", null);
        assertRejected(definition(AttributeValueType.NUMBER, null), Set.of(), value,
                "Attribute 'CODE' does not take a unit");
    }

    @Test
    void numberWithItsUnitIsAccepted() {
        AttributeValue value = new AttributeValue("attr_x", null, BigDecimal.TEN, null, null, "KG", null);
        assertThatCode(() -> AttributeValuePolicy.validate(
                definition(AttributeValueType.NUMBER, "WEIGHT_UNIT"), Set.of(), value))
                .doesNotThrowAnyException();
    }

    @Test
    void booleanWithoutAValueIsRejected() {
        assertRejected(definition(AttributeValueType.BOOLEAN, null), Set.of(), empty(),
                "A boolean value is required");
    }

    @Test
    void dateWithoutAValueIsRejected() {
        assertRejected(definition(AttributeValueType.DATE, null), Set.of(), empty(),
                "A date value is required");
    }

    @Test
    void dateValueIsAccepted() {
        AttributeValue value = new AttributeValue("attr_x", null, null, null, LocalDate.of(2026, 8, 12), null, null);
        assertThatCode(() -> AttributeValuePolicy.validate(definition(AttributeValueType.DATE, null), Set.of(), value))
                .doesNotThrowAnyException();
    }

    @Test
    void selectTakesExactlyOneOption() {
        assertThatCode(() -> AttributeValuePolicy.validate(
                definition(AttributeValueType.SELECT, null), OPTIONS, options("aopt_red")))
                .doesNotThrowAnyException();

        assertRejected(definition(AttributeValueType.SELECT, null), OPTIONS, options("aopt_red", "aopt_blue"),
                "Exactly one option must be selected");
    }

    @Test
    void selectWithoutAnOptionIsRejected() {
        assertRejected(definition(AttributeValueType.SELECT, null), OPTIONS, empty(),
                "At least one option must be selected");
    }

    @Test
    void anOptionOfAnotherAttributeIsRejected() {
        assertRejected(definition(AttributeValueType.SELECT, null), OPTIONS, options("aopt_green"),
                "Option 'aopt_green' does not belong to attribute 'CODE'");
    }

    @Test
    void multiSelectTakesSeveralDistinctOptions() {
        assertThatCode(() -> AttributeValuePolicy.validate(
                definition(AttributeValueType.MULTI_SELECT, null), OPTIONS, options("aopt_red", "aopt_blue")))
                .doesNotThrowAnyException();

        assertRejected(definition(AttributeValueType.MULTI_SELECT, null), OPTIONS, options("aopt_red", "aopt_red"),
                "The same option is selected more than once");
    }

    @Test
    void aLiteralSubmittedForAnOptionBasedAttributeIsRejected() {
        AttributeValue value = new AttributeValue("attr_x", "Red", null, null, null, null, List.of("aopt_red"));
        assertRejected(definition(AttributeValueType.SELECT, null), OPTIONS, value,
                "Attribute 'CODE' takes options, not a literal value");
    }

    @Test
    void anOptionSubmittedForALiteralAttributeIsRejected() {
        assertRejected(definition(AttributeValueType.TEXT, null), Set.of(), options("aopt_red"),
                "Attribute 'CODE' of type TEXT does not take options");
    }

    @Test
    void anInactiveAttributeTakesNoValues() {
        Attribute inactive = new Attribute(1L, "attr_x", 1L, null, "CODE", "Name", null, AttributeValueType.TEXT,
                AttributeScope.CATEGORY, null, null, false, AttributeStatus.INACTIVE, null, null);
        assertRejected(inactive, Set.of(), text("anything"),
                "Attribute 'CODE' is inactive and cannot take values");
    }

    private static void assertRejected(Attribute definition, Set<String> allowedOptionIds,
                                       AttributeValue value, String message) {
        assertThatThrownBy(() -> AttributeValuePolicy.validate(definition, allowedOptionIds, value))
                .isInstanceOf(InvalidAttributeRequestException.class)
                .hasMessage(message)
                .extracting("field").isEqualTo("attributes.CODE");
    }

    private static Attribute definition(AttributeValueType valueType, String unitDictionaryCode) {
        return new Attribute(1L, "attr_x", 1L, null, "CODE", "Name", null, valueType, AttributeScope.CATEGORY,
                unitDictionaryCode, null, false, AttributeStatus.ACTIVE, null, null);
    }

    private static AttributeValue text(String value) {
        return new AttributeValue("attr_x", value, null, null, null, null, null);
    }

    private static AttributeValue empty() {
        return new AttributeValue("attr_x", null, null, null, null, null, null);
    }

    private static AttributeValue options(String... optionIds) {
        return new AttributeValue("attr_x", null, null, null, null, null, List.of(optionIds));
    }
}
