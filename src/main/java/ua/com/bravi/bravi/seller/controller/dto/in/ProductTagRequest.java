package ua.com.bravi.bravi.seller.controller.dto.in;

import jakarta.validation.constraints.Size;
import ua.com.bravi.bravi.seller.tags.domain.TagName;

/**
 * One submitted tag: {@code id} addresses a tag the store already owns, {@code name} matches one by
 * name and mints it when nothing matches. Exactly one is expected; an entry carrying an id ignores
 * the name, since assigning a tag never renames it. Which one is missing is checked in the domain,
 * so the error can point at the entry that caused it.
 */
public record ProductTagRequest(
        String id,
        @Size(max = TagName.MAX_LENGTH)
        String name
) {
}
