package ua.com.bravi.bravi.seller.catalog.discounts.api;

import ua.com.bravi.bravi.seller.catalog.discounts.domain.Discount;
import ua.com.bravi.bravi.seller.catalog.discounts.domain.SubmittedDiscount;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Discounts of a product. This module never reads products: the caller resolves the product, proves
 * it belongs to the store and passes its internal id and price in. Keeping the edge one-way
 * (products depends on discounts, never the reverse) is what rules out a bean cycle — nothing
 * mechanical enforces it, because both packages sit inside the same application module.
 *
 * <p>Every method that depends on the current instant takes it explicitly, so a whole page is priced
 * against one moment and the rules stay deterministic under test.
 */
public interface DiscountsApi {

    /** The whole schedule of a product, earliest period first, with statuses resolved at {@code at}. */
    List<DiscountView> listForProduct(Long storeId, Long productId, Instant at);

    /**
     * Replaces a product's schedule. An entry carrying a public id keeps that row and its creation
     * time, an entry without one is created, and a stored row absent from the submission is removed —
     * which is the only way to stop a running discount early. Validated and written atomically.
     */
    List<DiscountView> replaceForProduct(Long storeId, Long productId, BigDecimal productPrice,
                                         List<SubmittedDiscount> submitted, Instant at);

    /** The discount in effect on one product, with the price it yields. Empty when nothing is running. */
    Optional<ProductDiscountView> activeForProduct(Long storeId, Long productId, BigDecimal price, Instant at);

    /**
     * Page enrichment: one query for every product on the page. Products with nothing in effect are
     * absent from the map rather than mapped to null.
     */
    Map<Long, ProductDiscountView> activeByProduct(Long storeId, List<DiscountTarget> targets, Instant at);

    /**
     * Adds the same discount to many products, skipping any where it would overlap an existing period
     * or would not stay below that product's price.
     */
    DiscountBulkResultView applyBulk(Long storeId, List<DiscountTarget> targets, Discount discount, Instant at);

    /** Rejects a new product price that a live fixed-amount discount would drive to zero or below. */
    void requireCompatibleWithPrice(Long storeId, Long productId, BigDecimal newPrice, Instant at);
}
