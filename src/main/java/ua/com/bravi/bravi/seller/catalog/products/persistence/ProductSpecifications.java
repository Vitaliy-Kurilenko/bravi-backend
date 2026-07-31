package ua.com.bravi.bravi.seller.catalog.products.persistence;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;
import ua.com.bravi.bravi.seller.catalog.products.domain.ProductSearchQuery;
import ua.com.bravi.bravi.seller.catalog.products.persistence.entity.ProductEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Builds a dynamic {@link Specification} that filters products by a {@link ProductSearchQuery}. */
public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    /**
     * {@code categoryIds} and {@code manufacturerIds} are already resolved internal bigint ids;
     * the product API translates public ids through the categories and manufacturers APIs beforehand.
     */
    public static Specification<ProductEntity> forStore(Long storeId, ProductSearchQuery query,
                                                        List<Long> categoryIds, List<Long> manufacturerIds) {
        return (root, criteriaQuery, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("storeId"), storeId));

            if (StringUtils.hasText(query.search())) {
                String like = "%" + query.search().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(root.get("code")), like),
                        cb.like(cb.lower(root.get("sku")), like)
                ));
            }
            addIn(predicates, root.get("categoryId"), categoryIds);
            addIn(predicates, root.get("manufacturerId"), manufacturerIds);
            addIn(predicates, root.get("stockStatusId"), query.stockStatusIds());
            addIn(predicates, root.get("status"), query.statuses());

            if (query.minPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), query.minPrice()));
            }
            if (query.maxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), query.maxPrice()));
            }
            if (query.createdFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), query.createdFrom()));
            }
            if (query.createdTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), query.createdTo()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static void addIn(List<Predicate> predicates, jakarta.persistence.criteria.Path<?> path,
                              Collection<?> values) {
        if (values != null && !values.isEmpty()) {
            predicates.add(path.in(values));
        }
    }
}
