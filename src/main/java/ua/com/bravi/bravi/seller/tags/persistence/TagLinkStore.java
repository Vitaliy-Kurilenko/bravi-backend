package ua.com.bravi.bravi.seller.tags.persistence;

import ua.com.bravi.bravi.seller.tags.domain.TagTarget;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * The one part of tagging that differs per target: where the links live. Each taggable aggregate
 * brings its own link table, because a single polymorphic table could not carry a foreign key to
 * two owners, and the cascade from the owner is what keeps links from outliving it.
 */
public interface TagLinkStore {

    TagTarget target();

    List<TagLink> findByOwnerIds(Collection<Long> ownerIds);

    void link(Long ownerId, Collection<Long> tagIds);

    void unlink(Long ownerId, Collection<Long> tagIds);

    /** Owner counts keyed by tag id; tags nobody carries are absent. */
    Map<Long, Long> countByTagIds(Collection<Long> tagIds);

    int repointToTag(Collection<Long> sourceIds, Long targetId);

    int deleteByTagIds(Collection<Long> tagIds);

    /** The link entity and its owner property, for a correlated subquery over an owner's search. */
    Class<?> linkEntity();

    String ownerProperty();
}
