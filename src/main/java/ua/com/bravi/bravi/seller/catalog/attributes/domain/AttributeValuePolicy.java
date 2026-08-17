package ua.com.bravi.bravi.seller.catalog.attributes.domain;

import ua.com.bravi.bravi.seller.catalog.attributes.exception.InvalidAttributeRequestException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Checks a submitted value against the attribute that defines it. Membership of a unit code in its
 * dictionary is left to the caller, which is the only check needing data from outside the catalog.
 */
public final class AttributeValuePolicy {

    public static final int MAX_TEXT_LENGTH = 4000;

    private AttributeValuePolicy() {
    }

    /** Field name reported in validation errors, addressing the attribute inside the submitted array. */
    public static String fieldOf(Attribute definition) {
        return "attributes." + definition.code();
    }

    /**
     * @param allowedOptionIds public ids of the options belonging to {@code definition}
     */
    public static void validate(Attribute definition, Set<String> allowedOptionIds, AttributeValue value) {
        String field = fieldOf(definition);

        if (definition.status() == AttributeStatus.INACTIVE) {
            throw new InvalidAttributeRequestException(field,
                    "Attribute '" + definition.code() + "' is inactive and cannot take values");
        }

        AttributeValueType type = definition.valueType();
        if (type.isOptionBased()) {
            validateOptions(definition, allowedOptionIds, value, field);
            return;
        }

        if (!value.optionIds().isEmpty()) {
            throw new InvalidAttributeRequestException(field,
                    "Attribute '" + definition.code() + "' of type " + type + " does not take options");
        }

        switch (type) {
            case TEXT -> validateText(value, field);
            case NUMBER -> validateNumber(definition, value, field);
            case BOOLEAN -> requirePresent(value.valueBoolean(), field, "A boolean value is required");
            case DATE -> requirePresent(value.valueDate(), field, "A date value is required");
            default -> throw new IllegalStateException("Unhandled attribute value type: " + type);
        }
    }

    private static void validateText(AttributeValue value, String field) {
        if (value.valueString() == null || value.valueString().isBlank()) {
            throw new InvalidAttributeRequestException(field, "A text value is required");
        }
        if (value.valueString().length() > MAX_TEXT_LENGTH) {
            throw new InvalidAttributeRequestException(field,
                    "Text value must not exceed " + MAX_TEXT_LENGTH + " characters");
        }
    }

    private static void validateNumber(Attribute definition, AttributeValue value, String field) {
        requirePresent(value.valueNumber(), field, "A numeric value is required");
        if (definition.unitDictionaryCode() != null && (value.unitCode() == null || value.unitCode().isBlank())) {
            throw new InvalidAttributeRequestException(field,
                    "A unit from the '" + definition.unitDictionaryCode() + "' dictionary is required");
        }
        if (definition.unitDictionaryCode() == null && value.unitCode() != null) {
            throw new InvalidAttributeRequestException(field,
                    "Attribute '" + definition.code() + "' does not take a unit");
        }
    }

    private static void validateOptions(Attribute definition, Set<String> allowedOptionIds,
                                        AttributeValue value, String field) {
        if (hasLiteral(value)) {
            throw new InvalidAttributeRequestException(field,
                    "Attribute '" + definition.code() + "' takes options, not a literal value");
        }
        List<String> submitted = value.optionIds();
        if (submitted.isEmpty()) {
            throw new InvalidAttributeRequestException(field, "At least one option must be selected");
        }
        if (!definition.valueType().allowsMultipleValues() && submitted.size() > 1) {
            throw new InvalidAttributeRequestException(field, "Exactly one option must be selected");
        }
        if (new HashSet<>(submitted).size() != submitted.size()) {
            throw new InvalidAttributeRequestException(field, "The same option is selected more than once");
        }
        for (String optionId : submitted) {
            if (!allowedOptionIds.contains(optionId)) {
                throw new InvalidAttributeRequestException(field,
                        "Option '" + optionId + "' does not belong to attribute '" + definition.code() + "'");
            }
        }
    }

    private static boolean hasLiteral(AttributeValue value) {
        return value.valueString() != null
                || value.valueNumber() != null
                || value.valueBoolean() != null
                || value.valueDate() != null;
    }

    private static void requirePresent(Object candidate, String field, String message) {
        if (candidate == null) {
            throw new InvalidAttributeRequestException(field, message);
        }
    }
}
