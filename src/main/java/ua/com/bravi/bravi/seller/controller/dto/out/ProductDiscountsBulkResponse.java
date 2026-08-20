package ua.com.bravi.bravi.seller.controller.dto.out;

import java.util.List;

/** How many products took the discount, and which ones did not. */
public record ProductDiscountsBulkResponse(
        int applied,
        List<SkippedProductResponse> skipped
) {
}
