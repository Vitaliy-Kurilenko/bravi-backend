package ua.com.bravi.bravi.seller.controller.dto.out;

import java.util.List;

/** Everything the product attribute editor needs: what the category offers and what is filled in. */
public record ProductAttributesResponse(
        List<CategoryAttributeResponse> offered,
        List<ProductAttributeValueResponse> values
) {
}
