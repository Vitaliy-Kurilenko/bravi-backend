package ua.com.bravi.bravi.seller.tags.persistence;

import jakarta.persistence.criteria.AbstractQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.stereotype.Component;
import ua.com.bravi.bravi.seller.tags.api.TagPredicates;
import ua.com.bravi.bravi.seller.tags.domain.TagTarget;
import ua.com.bravi.bravi.seller.tags.exception.InvalidTagRequestException;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class TagPredicatesImpl implements TagPredicates {

    private final Map<TagTarget, TagLinkStore> linkStores;

    public TagPredicatesImpl(List<TagLinkStore> linkStores) {
        this.linkStores = linkStores.stream()
                .collect(Collectors.toMap(TagLinkStore::target, Function.identity()));
    }

    @Override
    public Predicate taggedWith(TagTarget target, CriteriaBuilder cb, AbstractQuery<?> outerQuery,
                                Expression<Long> ownerIdPath, Collection<Long> tagIds, boolean matchAll) {
        TagLinkStore links = linkStores.get(target);
        if (links == null) {
            throw new InvalidTagRequestException("target", "Tags are not supported for " + target);
        }
        Set<Long> distinct = new LinkedHashSet<>(tagIds);

        if (!matchAll) {
            // Any: one matching link row is enough, so the subquery stops at the first hit.
            Subquery<Integer> any = outerQuery.subquery(Integer.class);
            Root<?> link = any.from(links.linkEntity());
            return cb.exists(any.select(cb.literal(1)).where(
                    cb.equal(link.get(links.ownerProperty()), ownerIdPath),
                    link.get("tagId").in(distinct)));
        }

        // All: counting the distinct matches takes one pass, where a chain of exists would take
        // one subquery per tag.
        Subquery<Long> all = outerQuery.subquery(Long.class);
        Root<?> link = all.from(links.linkEntity());
        all.select(cb.countDistinct(link.get("tagId"))).where(
                cb.equal(link.get(links.ownerProperty()), ownerIdPath),
                link.get("tagId").in(distinct));
        return cb.equal(all, cb.literal((long) distinct.size()));
    }
}
