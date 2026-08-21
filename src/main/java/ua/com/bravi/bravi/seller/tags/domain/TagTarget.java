package ua.com.bravi.bravi.seller.tags.domain;

import ua.com.bravi.bravi.seller.tags.exception.InvalidTagRequestException;

/**
 * What a tag labels. Products and orders keep separate vocabularies, so the target takes part in
 * the uniqueness of a tag name and is named explicitly by every operation on the dictionary.
 *
 * <p>{@code resource} is the RBAC resource guarding this target's endpoints. It is spelled out
 * rather than derived from the constant name, because the method security expressions read it.
 */
public enum TagTarget {
    PRODUCT("products", "PRODUCT"),
    ORDER("orders", "ORDER");

    private final String path;
    private final String resource;

    TagTarget(String path, String resource) {
        this.path = path;
        this.resource = resource;
    }

    /** The URL segment addressing this vocabulary. */
    public String path() {
        return path;
    }

    /**
     * The RBAC resource guarding this target's endpoints. Written out by hand, and read by the
     * method security expressions of the tag controller: renaming it moves the guard.
     */
    public String resource() {
        return resource;
    }

    /** Reads the URL segment, such as {@code products}, case-insensitively. */
    public static TagTarget fromPath(String token) {
        for (TagTarget value : values()) {
            if (value.path.equalsIgnoreCase(token) || value.name().equalsIgnoreCase(token)) {
                return value;
            }
        }
        throw new InvalidTagRequestException("target", "Unknown tag target: " + token);
    }
}
