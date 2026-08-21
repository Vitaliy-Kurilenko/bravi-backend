package ua.com.bravi.bravi.seller.tags.persistence;

/** One assignment, stripped of whichever table it came from. */
public record TagLink(Long id, Long ownerId, Long tagId) {
}
