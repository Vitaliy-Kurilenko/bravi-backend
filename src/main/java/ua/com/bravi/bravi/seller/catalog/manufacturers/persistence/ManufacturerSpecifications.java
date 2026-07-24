package ua.com.bravi.bravi.seller.catalog.manufacturers.persistence;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;
import ua.com.bravi.bravi.seller.catalog.manufacturers.domain.ManufacturerSearchQuery;
import ua.com.bravi.bravi.seller.catalog.manufacturers.persistence.entity.ManufacturerEntity;

import java.util.ArrayList;
import java.util.List;

/** Будує динамічний {@link Specification} фільтрації виробників із {@link ManufacturerSearchQuery}. */
public final class ManufacturerSpecifications {

    private ManufacturerSpecifications() {
    }

    public static Specification<ManufacturerEntity> forStore(Long storeId, ManufacturerSearchQuery query) {
        return (root, criteriaQuery, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("storeId"), storeId));

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
