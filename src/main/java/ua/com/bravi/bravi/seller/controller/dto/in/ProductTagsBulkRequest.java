package ua.com.bravi.bravi.seller.controller.dto.in;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import ua.com.bravi.bravi.seller.tags.domain.TagBulkMode;

import java.util.List;

/** Applies one set of tags to many products. {@code mode} defaults to ADD. */
public record ProductTagsBulkRequest(
        @JsonProperty("product_ids")
        @NotEmpty
        @Size(max = 200)
        List<String> productIds,
        @NotEmpty
        @Valid
        List<ProductTagRequest> tags,
        TagBulkMode mode
) {
}
