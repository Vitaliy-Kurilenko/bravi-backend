package ua.com.bravi.bravi.seller.controller.dto.in;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import ua.com.bravi.bravi.seller.catalog.products.domain.ProductStatus;

import java.math.BigDecimal;
import java.util.List;

public record ProductCreateRequest(
        @NotBlank
        String name,
        String sku,
        @NotBlank
        String code,
        String description,
        @JsonProperty("category_id")
        String categoryId,
        @JsonProperty("manufacturer_id")
        String manufacturerId,
        @JsonProperty("stock_status_id")
        @NotNull
        Long stockStatusId,
        @NotNull
        @PositiveOrZero
        BigDecimal price,
        @NotNull
        @PositiveOrZero
        Integer quantity,
        BigDecimal weight,
        BigDecimal width,
        BigDecimal height,
        BigDecimal length,
        ProductStatus status,
        @Valid
        List<ProductAttributeValueRequest> attributes
) {
}
