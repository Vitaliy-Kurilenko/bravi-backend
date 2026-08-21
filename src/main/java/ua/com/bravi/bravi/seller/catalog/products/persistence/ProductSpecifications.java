package ua.com.bravi.bravi.seller.catalog.products.persistence;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;
import ua.com.bravi.bravi.seller.catalog.discounts.api.DiscountPredicates;
import ua.com.bravi.bravi.seller.catalog.products.domain.ProductFilterRefs;
import ua.com.bravi.bravi.seller.catalog.products.domain.ProductSearchQuery;
import ua.com.bravi.bravi.seller.catalog.products.persistence.entity.ProductEntity;
import ua.com.bravi.bravi.seller.tags.api.TagPredicates;
import ua.com.bravi.bravi.seller.tags.domain.TagTarget;
import ua.com.bravi.bravi.seller.tags.domain.TagsMatch;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Builds a dynamic {@link Specification} that filters products by a {@link ProductSearchQuery}. */
public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    /**
     * {@code refs} carries filters already resolved into internal bigint ids; the product API
     * translates the public ids through the neighbouring APIs beforehand. {@code at} is passed in
     * rather than sampled here so a page is filtered and priced by one instant.
     */
    public static Specification<ProductEntity> forStore(Long storeId, ProductSearchQuery query,
                                                        ProductFilterRefs refs,
                                                        DiscountPredicates discountPredicates,
                                                        TagPredicates tagPredicates, Instant at) {
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
            addIn(predicates, root.get("categoryId"), refs.categoryIds());
            addIn(predicates, root.get("manufacturerId"), refs.manufacturerIds());
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
            // criteriaQuery is null on the exists() path, which never carries this filter.
            if (query.hasActiveDiscount() != null && criteriaQuery != null) {
                Predicate discounted = discountPredicates.activeAt(cb, criteriaQuery, root.get("id"), at);
                predicates.add(query.hasActiveDiscount() ? discounted : cb.not(discounted));
            }
            if (refs.tagIds() != null && !refs.tagIds().isEmpty() && criteriaQuery != null) {
                predicates.add(tagPredicates.taggedWith(TagTarget.PRODUCT, cb, criteriaQuery,
                        root.get("id"), refs.tagIds(), query.tagsMatch() == TagsMatch.ALL));
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
