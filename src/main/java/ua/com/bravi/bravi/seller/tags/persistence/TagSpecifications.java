package ua.com.bravi.bravi.seller.tags.persistence;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;
import ua.com.bravi.bravi.seller.tags.domain.TagSearchQuery;
import ua.com.bravi.bravi.seller.tags.domain.TagTarget;
import ua.com.bravi.bravi.seller.tags.persistence.entity.TagEntity;

import java.util.ArrayList;
import java.util.List;

/** Builds a dynamic {@link Specification} that filters tags by a {@link TagSearchQuery}. */
public final class TagSpecifications {

    private TagSpecifications() {
    }

    public static Specification<TagEntity> forStore(Long storeId, TagTarget target, TagSearchQuery query) {
        return (root, criteriaQuery, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("storeId"), storeId));
            predicates.add(cb.equal(root.get("target"), target));

            if (StringUtils.hasText(query.search())) {
                String like = "%" + query.search().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("name")), like));
            }
            if (query.statuses() != null && !query.statuses().isEmpty()) {
                predicates.add(root.get("status").in(query.statuses()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
