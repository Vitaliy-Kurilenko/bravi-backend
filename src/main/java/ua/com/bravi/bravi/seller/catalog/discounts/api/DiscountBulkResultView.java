package ua.com.bravi.bravi.seller.catalog.discounts.api;

import java.util.List;

/** Outcome of applying one discount to many products. */
public record DiscountBulkResultView(int applied, List<SkippedProductView> skipped) {
}
