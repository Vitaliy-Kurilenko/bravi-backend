package ua.com.bravi.bravi.seller.catalog.attributes.domain;

/** Why an attribute appears in a category's effective set, which is what tells the seller where to edit it. */
public enum AttributeSource {

    /** Reaches every product of the store; no category binding involved. */
    GLOBAL,

    /** Bound to an ancestor category and inherited from there. */
    INHERITED,

    /** Bound to this very category. */
    OWN
}
