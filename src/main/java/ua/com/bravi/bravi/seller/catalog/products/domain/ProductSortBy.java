package ua.com.bravi.bravi.seller.catalog.products.domain;

import lombok.Getter;
import ua.com.bravi.bravi.seller.catalog.products.exception.InvalidProductRequestException;

/**
 * Whitelist полів сортування товарів. {@code property} — ім'я властивості {@code ProductEntity}
 * для безпечного {@code Sort} (без прямого прокидання довільного рядка з клієнта).
 */
@Getter
public enum ProductSortBy {
    ID("id"),
    NAME("name"),
    PRICE("price"),
    QUANTITY("quantity"),
    STOCK_STATUS_ID("stockStatusId"),
    CREATED_AT("createdAt"),
    UPDATED_AT("updatedAt"),
    MANUFACTURER_ID("manufacturerId");

    private final String property;

    ProductSortBy(String property) {
        this.property = property;
    }

    /** Парсить зовнішній токен (наприклад {@code created_at}) у enum, case-insensitive. */
    public static ProductSortBy fromParam(String token) {
        for (ProductSortBy value : values()) {
            if (value.name().equalsIgnoreCase(token)) {
                return value;
            }
        }
        throw new InvalidProductRequestException("sort_by", "Unknown sort field: " + token);
    }
}
