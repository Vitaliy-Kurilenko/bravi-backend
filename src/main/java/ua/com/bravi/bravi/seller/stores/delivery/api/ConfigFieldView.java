package ua.com.bravi.bravi.seller.stores.delivery.api;

public record ConfigFieldView(
        String key,
        String label,
        boolean required,
        String type
) {
}
