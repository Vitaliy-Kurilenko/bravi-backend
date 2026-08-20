package ua.com.bravi.bravi.seller.catalog.discounts.persistence;

import jakarta.persistence.criteria.AbstractQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.stereotype.Component;
import ua.com.bravi.bravi.seller.catalog.discounts.api.DiscountPredicates;
import ua.com.bravi.bravi.seller.catalog.discounts.persistence.entity.ProductDiscountEntity;

import java.time.Instant;

@Component
public class DiscountPredicatesImpl implements DiscountPredicates {

    @Override
    public Predicate activeAt(CriteriaBuilder cb, AbstractQuery<?> outerQuery,
                              Expression<Long> productIdPath, Instant at) {
        Subquery<Integer> subquery = outerQuery.subquery(Integer.class);
        Root<ProductDiscountEntity> discount = subquery.from(ProductDiscountEntity.class);
        return cb.exists(subquery.select(cb.literal(1)).where(
                cb.equal(discount.get("productId"), productIdPath),
                cb.lessThanOrEqualTo(discount.get("startsAt"), at),
                cb.or(cb.isNull(discount.get("endsAt")), cb.greaterThan(discount.get("endsAt"), at))));
    }
}
