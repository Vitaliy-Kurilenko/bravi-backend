package ua.com.bravi.bravi.seller.catalog.discounts.api;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.AbstractQuery;
import jakarta.persistence.criteria.Predicate;

import java.time.Instant;

/**
 * Contributes a correlated EXISTS over the discount table to another aggregate's Specification.
 * Only standard criteria types cross the boundary; the discount entity stays inside this module.
 *
 * <p>A subquery rather than a list of product ids because filtering and paging have to happen in one
 * statement: handing back the ids of every discounted product degrades into an unbounded IN list on
 * a store that runs promotions across its catalogue.
 */
public interface DiscountPredicates {

    Predicate activeAt(CriteriaBuilder cb, AbstractQuery<?> outerQuery, Expression<Long> productIdPath, Instant at);
}
