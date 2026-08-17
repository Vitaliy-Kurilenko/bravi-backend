package ua.com.bravi.bravi.seller.catalog.categories.api;

/** One node of a category's ancestor chain, flat so consumers need not walk the recursive tree view. */
public record CategoryPathEntry(Long id, String publicId, String name) {
}
