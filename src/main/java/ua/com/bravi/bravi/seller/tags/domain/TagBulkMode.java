package ua.com.bravi.bravi.seller.tags.domain;

/** What a bulk assignment does to the tags an owner already carries. */
public enum TagBulkMode {
    /** Attaches the submitted tags, keeping the rest. */
    ADD,
    /** Detaches the submitted tags, keeping the rest. */
    REMOVE,
    /** Leaves the owner with exactly the submitted tags. */
    REPLACE
}
