package ua.com.bravi.bravi.seller.catalog.attributes.domain;

/** Shape of the values an attribute accepts, which decides both validation and the column a value lands in. */
public enum AttributeValueType {

    TEXT,
    NUMBER,
    BOOLEAN,
    DATE,
    SELECT,
    MULTI_SELECT;

    /** Tells whether values of this type point at an option of the attribute rather than carrying a literal. */
    public boolean isOptionBased() {
        return this == SELECT || this == MULTI_SELECT;
    }

    /** Tells whether a product may hold more than one value of this type. */
    public boolean allowsMultipleValues() {
        return this == MULTI_SELECT;
    }
}
