package ua.com.bravi.bravi.seller.catalog.manufacturers.domain;

import lombok.Getter;
import ua.com.bravi.bravi.seller.catalog.manufacturers.exception.InvalidManufacturerRequestException;

/**
 * Whitelist полів сортування виробників. {@code property} — ім'я властивості {@code ManufacturerEntity}
 * для безпечного {@code Sort} (без прямого прокидання довільного рядка з клієнта).
 */
@Getter
public enum ManufacturerSortBy {
    ID("id"),
    NAME("name"),
    STATUS("status"),
    CREATED_AT("createdAt");

    private final String property;

    ManufacturerSortBy(String property) {
        this.property = property;
    }

    /** Парсить зовнішній токен (наприклад {@code created_at}) у enum, case-insensitive. */
    public static ManufacturerSortBy fromParam(String token) {
        for (ManufacturerSortBy value : values()) {
            if (value.name().equalsIgnoreCase(token)) {
                return value;
            }
        }
        throw new InvalidManufacturerRequestException("sort_by", "Unknown sort field: " + token);
    }
}
