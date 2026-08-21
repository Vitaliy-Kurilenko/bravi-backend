package ua.com.bravi.bravi.seller.tags.api;

import jakarta.persistence.criteria.AbstractQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import ua.com.bravi.bravi.seller.tags.domain.TagTarget;

import java.util.Collection;

/**
 * Lets an owning aggregate filter its own search by tags without seeing this module's entities.
 * The predicate is a correlated subquery rather than a list of owner ids, so a store that tags its
 * whole catalogue does not turn the filter into an unbounded {@code IN}.
 */
public interface TagPredicates {

    /**
     * @param matchAll {@code true} demands every tag, {@code false} any one of them
     */
    Predicate taggedWith(TagTarget target, CriteriaBuilder cb, AbstractQuery<?> outerQuery,
                         Expression<Long> ownerIdPath, Collection<Long> tagIds, boolean matchAll);
}
