package ua.com.bravi.bravi.seller.stores.contacts.domain;

import ua.com.bravi.bravi.seller.stores.contacts.exception.InvalidContactValueException;
import ua.com.bravi.bravi.shared.util.ValidationPatterns;

public final class StoreContactPolicy {

    private static final String FIELD_VALUE = "value";

    public static void validate(ContactType type, String value) {
        if (type == null) {
            throw new InvalidContactValueException("type", "Contact type must not be null");
        }
        if (value == null || value.isBlank()) {
            throw new InvalidContactValueException(FIELD_VALUE, "Contact value must not be blank");
        }

        switch (type) {
            case EMAIL -> require(ValidationPatterns.EMAIL.matcher(value).matches(),
                    "Value must be a valid email address");
            case WEBSITE -> require(ValidationPatterns.URL.matcher(value).matches(),
                    "Value must be a valid http/https URL");
            case PHONE, VIBER, WHATSAPP -> require(ValidationPatterns.PHONE.matcher(value).matches(),
                    "Value must be a valid phone number");
            case TELEGRAM -> require(
                    ValidationPatterns.TELEGRAM_USERNAME.matcher(value).matches()
                            || ValidationPatterns.PHONE.matcher(value).matches(),
                    "Value must be a Telegram @username or a valid phone number");
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new InvalidContactValueException(FIELD_VALUE, message);
        }
    }

    private StoreContactPolicy() {
    }
}
