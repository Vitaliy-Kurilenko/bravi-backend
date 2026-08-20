package ua.com.bravi.bravi.seller.controller.dto.in;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** The product's whole discount schedule; an empty list clears it. */
public record ProductDiscountsReplaceRequest(
        @NotNull @Valid List<ProductDiscountRequest> discounts
) {
}
