package ua.com.bravi.bravi.seller.catalog.discounts.domain;

/** Derived from the period and the current instant; never stored and never submitted. */
public enum DiscountStatus {
    SCHEDULED,
    ACTIVE,
    EXPIRED
}
