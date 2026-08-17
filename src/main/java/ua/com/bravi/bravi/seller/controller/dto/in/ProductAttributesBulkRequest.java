package ua.com.bravi.bravi.seller.controller.dto.in;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** Applies the same values to many products at once, leaving their other attributes untouched. */
public record ProductAttributesBulkRequest(
        @NotEmpty @JsonProperty("product_ids") List<String> productIds,
        @NotEmpty @Valid List<ProductAttributeValueRequest> attributes
) {
}
