package ua.com.bravi.bravi.stores.payments.api;

public record ConfigFieldView(
        String key,
        String label,
        boolean required,
        String type
) {
}
