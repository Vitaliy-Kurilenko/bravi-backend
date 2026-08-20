package ua.com.bravi.bravi.seller.catalog.discounts.domain;

import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** The discounts of one product, ordered by when they start. */
public record DiscountSchedule(List<Discount> discounts) {

    public static DiscountSchedule of(Collection<Discount> discounts) {
        return new DiscountSchedule(discounts.stream()
                .sorted(Comparator.comparing(Discount::startsAt))
                .toList());
    }

    /** The discount in effect at {@code now}; the non-overlap invariant makes it at most one. */
    public Optional<Discount> activeAt(Instant now) {
        return discounts.stream().filter(discount -> discount.activeAt(now)).findFirst();
    }

    /**
     * The earliest stored discount whose period collides with {@code candidate}, skipping the row the
     * candidate itself came from so an edit never conflicts with its own stored state.
     */
    public Optional<Discount> conflictWith(DiscountPeriod candidate, Long ignoreId) {
        return discounts.stream()
                .filter(discount -> !Objects.equals(discount.id(), ignoreId))
                .filter(discount -> discount.period().overlaps(candidate))
                .findFirst();
    }

    /** Discounts that have not ended at {@code now} — what a price change has to stay compatible with. */
    public List<Discount> liveAt(Instant now) {
        return discounts.stream()
                .filter(discount -> !discount.period().entirelyBefore(now))
                .toList();
    }
}
