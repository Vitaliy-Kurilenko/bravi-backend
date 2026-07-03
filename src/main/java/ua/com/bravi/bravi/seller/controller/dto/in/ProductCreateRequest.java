package ua.com.bravi.bravi.seller.controller.dto.in;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import ua.com.bravi.bravi.seller.catalog.products.domain.ProductStatus;

import java.math.BigDecimal;

public record ProductCreateRequest(
        @NotBlank
        String name,
        String sku,
        @NotBlank
        String code,
        String description,
        @JsonProperty("category_id")
        Long categoryId,
        @JsonProperty("manufacturer_id")
        Long manufacturerId,
        @JsonProperty("stock_status_id")
        @NotNull
        Long stockStatusId,
        @JsonProperty("partner_price")
        @NotNull
        @PositiveOrZero
        BigDecimal partnerPrice,
        @JsonProperty("recommended_price")
        @NotNull
        @PositiveOrZero
        BigDecimal recommendedPrice,
        @NotNull
        @PositiveOrZero
        Integer quantity,
        BigDecimal weight,
        BigDecimal width,
        BigDecimal height,
        BigDecimal length,
        ProductStatus status
) {
}
