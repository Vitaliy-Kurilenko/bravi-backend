package ua.com.bravi.bravi.seller.tags.domain;

/**
 * One submitted tag: either an existing tag addressed by its public id, or a name to match against
 * the store's tags and to create when nothing matches. Exactly one of the two is expected; an
 * entry carrying an id ignores the name, because assigning a tag never renames it.
 */
public record TagRef(
        String id,
        String name
) {
}
