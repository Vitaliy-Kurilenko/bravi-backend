package ua.com.bravi.bravi.seller.catalog.attributes.persistence;

import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;
import ua.com.bravi.bravi.seller.catalog.attributes.domain.AttributeSearchQuery;
import ua.com.bravi.bravi.seller.catalog.attributes.persistence.entity.AttributeEntity;

import java.util.ArrayList;
import java.util.List;

/** Builds a dynamic {@link Specification} that filters attributes by an {@link AttributeSearchQuery}. */
public final class AttributeSpecifications {

    private AttributeSpecifications() {
    }

    public static Specification<AttributeEntity> forStore(Long storeId, AttributeSearchQuery query) {
        return (root, criteriaQuery, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("storeId"), storeId));

            if (StringUtils.hasText(query.search())) {
                String like = "%" + query.search().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(root.get("code")), like)));
            }
            addIn(predicates, root.get("valueType"), query.valueTypes());
            addIn(predicates, root.get("scope"), query.scopes());
            addIn(predicates, root.get("status"), query.statuses());

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static void addIn(List<Predicate> predicates, Path<?> path, List<?> values) {
        if (values != null && !values.isEmpty()) {
            predicates.add(path.in(values));
        }
    }
}
