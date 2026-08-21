package ua.com.bravi.bravi.seller.controller.dto.in;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import ua.com.bravi.bravi.seller.catalog.products.domain.ProductStatus;

import java.math.BigDecimal;
import java.util.List;

/**
 * {@code attributes} and {@code tags}, when present, replace the product's whole set. An absent
 * field leaves what the product carries alone; an empty list clears it.
 */
public record ProductUpdateRequest(
        String name,
        String sku,
        String code,
        String description,
        @JsonProperty("category_id")
        String categoryId,
        @JsonProperty("manufacturer_id")
        String manufacturerId,
        @JsonProperty("stock_status_id")
        Long stockStatusId,
        @PositiveOrZero
        BigDecimal price,
        @PositiveOrZero
        Integer quantity,
        BigDecimal weight,
        BigDecimal width,
        BigDecimal height,
        BigDecimal length,
        ProductStatus status,
        @Valid
        List<ProductAttributeValueRequest> attributes,
        @Valid
        List<ProductTagRequest> tags
) {
}
