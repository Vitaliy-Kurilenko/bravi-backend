package ua.com.bravi.bravi.seller.catalog.discounts.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * The window a discount is in effect for, half-open as {@code [startsAt, ends_at)}: two periods that
 * merely touch at a boundary do not collide. A null {@code endsAt} runs to the end of the axis, which
 * is what makes an open-ended discount collide with every period that starts after it.
 */
public record DiscountPeriod(Instant startsAt, Instant endsAt) {

    public boolean openEnded() {
        return endsAt == null;
    }

    public boolean contains(Instant at) {
        return !at.isBefore(startsAt) && (endsAt == null || at.isBefore(endsAt));
    }

    public boolean overlaps(DiscountPeriod other) {
        return startsBeforeEndOf(this, other) && startsBeforeEndOf(other, this);
    }

    /** True once the period is over; an open-ended one never is. */
    public boolean entirelyBefore(Instant at) {
        return endsAt != null && !endsAt.isAfter(at);
    }

    public DiscountStatus statusAt(Instant now) {
        if (now.isBefore(startsAt)) {
            return DiscountStatus.SCHEDULED;
        }
        return contains(now) ? DiscountStatus.ACTIVE : DiscountStatus.EXPIRED;
    }

    public boolean sameAs(DiscountPeriod other) {
        return Objects.equals(startsAt, other.startsAt) && Objects.equals(endsAt, other.endsAt);
    }

    private static boolean startsBeforeEndOf(DiscountPeriod one, DiscountPeriod two) {
        return two.endsAt == null || one.startsAt.isBefore(two.endsAt);
    }
}
