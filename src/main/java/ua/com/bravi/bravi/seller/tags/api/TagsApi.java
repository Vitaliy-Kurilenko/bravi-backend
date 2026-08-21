package ua.com.bravi.bravi.seller.tags.api;

import ua.com.bravi.bravi.seller.tags.domain.Tag;
import ua.com.bravi.bravi.seller.tags.domain.TagBulkMode;
import ua.com.bravi.bravi.seller.tags.domain.TagRef;
import ua.com.bravi.bravi.seller.tags.domain.TagSearchQuery;
import ua.com.bravi.bravi.seller.tags.domain.TagTarget;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * The store's tag dictionary and the tags pinned on its things. One dictionary serves every kind
 * of taggable thing: the target names which vocabulary an operation works in, and an operation
 * never reaches a tag of another target.
 *
 * <p>This module never reads products or orders. The caller resolves the owner, proves it belongs
 * to the store and passes its internal id in, which keeps the edge one-way and rules out a cycle.
 */
public interface TagsApi {

    TagPage search(Long storeId, TagTarget target, TagSearchQuery query);

    TagView getByPublicId(Long storeId, TagTarget target, String publicId);

    TagView create(Long storeId, TagTarget target, Tag tag);

    void update(Long storeId, TagTarget target, String publicId, Tag patch);

    /** Deletes the tag, untagging everything it labelled. */
    void delete(Long storeId, TagTarget target, String publicId);

    /** Moves every assignment of the sources onto the target and deletes the sources. */
    TagView merge(Long storeId, TagTarget target, String targetPublicId, List<String> sourcePublicIds);

    List<TagView> listFor(Long storeId, TagTarget target, Long ownerId);

    /** Page enrichment: one pass for every owner on the page. Untagged owners are absent. */
    Map<Long, List<TagView>> listByOwner(Long storeId, TagTarget target, Collection<Long> ownerIds);

    /** Leaves the owner with exactly the submitted tags, minting any name the store does not own yet. */
    List<TagView> replaceFor(Long storeId, TagTarget target, Long ownerId, List<TagRef> tags);

    /** Returns how many owners actually changed. */
    int applyBulk(Long storeId, TagTarget target, List<Long> ownerIds, List<TagRef> tags, TagBulkMode mode);

    /** Public ids to internal ids for an owner's search filter; 404 on an id the store does not own. */
    List<Long> resolveFilter(Long storeId, TagTarget target, List<String> tagPublicIds);
}
