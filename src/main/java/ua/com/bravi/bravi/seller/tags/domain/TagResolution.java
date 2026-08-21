package ua.com.bravi.bravi.seller.tags.domain;

import ua.com.bravi.bravi.seller.tags.exception.InvalidTagRequestException;
import ua.com.bravi.bravi.shared.exception.NotFoundException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * What a submitted selection means for the dictionary: the tags that already exist and the names
 * that have to be minted. Entries keep their submitted order, and an entry naming a tag the store
 * already owns never creates a second one, whatever its capitalization or spacing.
 */
public record TagResolution(List<Tag> existing, List<String> newNames) {

    /**
     * @param submitted entries as the seller sent them, order preserved
     * @param storeTags the store's tags for this target, pre-loaded by the caller
     * @throws NotFoundException          an entry names a public id the store does not own
     * @throws InvalidTagRequestException an entry carries neither an id nor a usable name
     */
    public static TagResolution plan(List<TagRef> submitted, Collection<Tag> storeTags) {
        Map<String, Tag> byPublicId = new HashMap<>();
        Map<String, Tag> byNameKey = new HashMap<>();
        for (Tag tag : storeTags) {
            byPublicId.put(tag.publicId(), tag);
            byNameKey.put(TagName.key(tag.name()), tag);
        }

        Map<String, Tag> resolved = new LinkedHashMap<>();
        Map<String, String> pending = new LinkedHashMap<>();

        for (int index = 0; index < submitted.size(); index++) {
            TagRef ref = submitted.get(index);
            if (ref == null) {
                throw new InvalidTagRequestException("tags[" + index + "]", "A tag id or a name is required");
            }
            if (hasText(ref.id())) {
                Tag tag = byPublicId.get(ref.id());
                if (tag == null) {
                    throw new NotFoundException("Tag not found: " + ref.id());
                }
                resolved.put(tag.publicId(), tag);
                continue;
            }
            if (!hasText(ref.name())) {
                throw new InvalidTagRequestException("tags[" + index + "]", "A tag id or a name is required");
            }
            String name = TagName.normalize(ref.name(), TagName.fieldOf(index, "name"));
            String key = TagName.key(name);
            Tag tag = byNameKey.get(key);
            if (tag != null) {
                resolved.put(tag.publicId(), tag);
            } else {
                pending.putIfAbsent(key, name);
            }
        }

        Set<String> resolvedKeys = new LinkedHashSet<>();
        for (Tag tag : resolved.values()) {
            resolvedKeys.add(TagName.key(tag.name()));
        }
        List<String> newNames = new ArrayList<>();
        pending.forEach((key, name) -> {
            if (!resolvedKeys.contains(key)) {
                newNames.add(name);
            }
        });

        return new TagResolution(List.copyOf(resolved.values()), List.copyOf(newNames));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
