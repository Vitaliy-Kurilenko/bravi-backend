package ua.com.bravi.bravi.seller.catalog.attributes.domain;

/** Decides which products an attribute reaches. */
public enum AttributeScope {

    /** Offered to every product of the store, including products without a category. */
    GLOBAL,

    /** Offered only to products in the categories the attribute is bound to, and their descendants. */
    CATEGORY
}
