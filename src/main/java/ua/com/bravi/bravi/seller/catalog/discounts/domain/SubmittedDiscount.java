package ua.com.bravi.bravi.seller.catalog.discounts.domain;

/**
 * A discount as it arrived in a submitted schedule, keeping its position in the array so a validation
 * error can address the exact entry the seller typed.
 */
public record SubmittedDiscount(int index, Discount discount) {
}
