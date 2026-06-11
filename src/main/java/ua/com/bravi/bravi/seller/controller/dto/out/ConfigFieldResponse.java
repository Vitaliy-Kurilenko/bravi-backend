package ua.com.bravi.bravi.seller.controller.dto.out;

public record ConfigFieldResponse(
        String key,
        String label,
        boolean required,
        String type
) {
}
