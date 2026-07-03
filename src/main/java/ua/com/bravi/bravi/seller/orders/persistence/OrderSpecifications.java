package ua.com.bravi.bravi.seller.orders.persistence;

import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;
import ua.com.bravi.bravi.seller.orders.domain.OrderSearchQuery;
import ua.com.bravi.bravi.seller.orders.persistence.entity.OrderEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Будує динамічний {@link Specification} фільтрації замовлень із {@link OrderSearchQuery}. */
public final class OrderSpecifications {

    private OrderSpecifications() {
    }

    public static Specification<OrderEntity> forStore(Long storeId, OrderSearchQuery query) {
        return (root, criteriaQuery, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("storeId"), storeId));

            if (StringUtils.hasText(query.search())) {
                String like = "%" + query.search().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("recipientPhone")), like),
                        cb.like(cb.lower(root.get("recipientFirstName")), like),
                        cb.like(cb.lower(root.get("recipientLastName")), like),
                        cb.like(cb.lower(root.get("recipientEmail")), like)
                ));
            }

            addIn(predicates, root.get("buyerId"), query.buyerIds());
            addIn(predicates, root.get("paymentMethodCode"), query.paymentMethodCodes());
            addIn(predicates, root.get("deliveryMethodCode"), query.deliveryMethodCodes());

            if (StringUtils.hasText(query.recipientName())) {
                String like = "%" + query.recipientName().toLowerCase() + "%";
                var fullName = cb.lower(cb.concat(cb.concat(root.get("recipientFirstName"), " "),
                        root.get("recipientLastName")));
                predicates.add(cb.like(fullName, like));
            }
            if (StringUtils.hasText(query.recipientPhone())) {
                predicates.add(cb.like(cb.lower(root.get("recipientPhone")),
                        "%" + query.recipientPhone().toLowerCase() + "%"));
            }
            if (StringUtils.hasText(query.recipientEmail())) {
                predicates.add(cb.like(cb.lower(root.get("recipientEmail")),
                        "%" + query.recipientEmail().toLowerCase() + "%"));
            }

            if (query.statusCodes() != null && !query.statusCodes().isEmpty()) {
                predicates.add(root.get("status").get("code").in(query.statusCodes()));
            }

            if (query.minTotal() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("total"), query.minTotal()));
            }
            if (query.maxTotal() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("total"), query.maxTotal()));
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

    private static void addIn(List<Predicate> predicates, Path<?> path, Collection<?> values) {
        if (values != null && !values.isEmpty()) {
            predicates.add(path.in(values));
        }
    }
}
