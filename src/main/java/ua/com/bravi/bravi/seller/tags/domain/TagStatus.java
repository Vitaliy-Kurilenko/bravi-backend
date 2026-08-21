package ua.com.bravi.bravi.seller.tags.domain;

/**
 * Whether the tag is offered when picking tags. An inactive tag keeps its assignments and still
 * matches a submitted name, so deactivating one never turns a later save into a failure.
 */
public enum TagStatus {
    ACTIVE,
    INACTIVE
}
