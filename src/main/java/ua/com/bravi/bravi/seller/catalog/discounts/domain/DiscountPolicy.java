package ua.com.bravi.bravi.seller.catalog.discounts.domain;

import ua.com.bravi.bravi.seller.catalog.discounts.exception.DiscountOverlapException;
import ua.com.bravi.bravi.seller.catalog.discounts.exception.InvalidDiscountRequestException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Every rule a discount has to satisfy, expressed over data the caller has already loaded.
 *
 * <p>A replacement validates only what actually changed, and always validates the resulting set for
 * overlap. That distinction is what lets a seller resubmit an untouched schedule: an expired discount
 * sent back verbatim must not trip the rule forbidding a period that is entirely in the past.
 */
public final class DiscountPolicy {

    public static final BigDecimal MIN_PERCENT = new BigDecimal("0.01");
    public static final BigDecimal MAX_PERCENT = new BigDecimal("99");

    private DiscountPolicy() {
    }

    /** Addresses one entry of the submitted array, e.g. {@code discounts[2].starts_at}. */
    public static String fieldOf(int index, String property) {
        return "discounts[" + index + "]." + property;
    }

    public static void validateValue(int index, DiscountType type, BigDecimal value, BigDecimal productPrice) {
        validateValue(fieldOf(index, "value"), type, value, productPrice);
    }

    /**
     * Field-name overload for a request that submits one discount on its own rather than as an entry of
     * a schedule, so the error points at the property the client actually sent.
     */
    public static void validateValue(String field, DiscountType type, BigDecimal value, BigDecimal productPrice) {
        if (type == DiscountType.PERCENT) {
            if (value.compareTo(MIN_PERCENT) < 0 || value.compareTo(MAX_PERCENT) > 0) {
                throw new InvalidDiscountRequestException(field,
                        "Percent discount must be between 0.01 and 99");
            }
            return;
        }
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidDiscountRequestException(field, "Amount discount must be greater than zero");
        }
        if (value.compareTo(productPrice) >= 0) {
            throw new InvalidDiscountRequestException(field,
                    "Discount is larger than the product price " + productPrice.stripTrailingZeros().toPlainString());
        }
    }

    public static void validatePeriod(int index, DiscountPeriod period) {
        validatePeriod(fieldOf(index, "ends_at"), period);
    }

    public static void validatePeriod(String field, DiscountPeriod period) {
        if (period.endsAt() != null && !period.endsAt().isAfter(period.startsAt())) {
            throw new InvalidDiscountRequestException(field, "End of the period must be after its start");
        }
    }

    public static void requireNotEntirelyPast(int index, DiscountPeriod period, Instant now) {
        requireNotEntirelyPast(fieldOf(index, "ends_at"), period, now);
    }

    public static void requireNotEntirelyPast(String field, DiscountPeriod period, Instant now) {
        if (period.entirelyBefore(now)) {
            throw new InvalidDiscountRequestException(field, "The period is entirely in the past");
        }
    }

    /**
     * A running discount may change everything except when it started; removing it is the way to stop
     * it early.
     */
    public static void requireStartUnchangedWhileActive(int index, Discount stored,
                                                        DiscountPeriod submitted, Instant now) {
        if (stored.activeAt(now) && !Objects.equals(stored.startsAt(), submitted.startsAt())) {
            throw new InvalidDiscountRequestException(fieldOf(index, "starts_at"),
                    "The start of a running discount cannot be moved");
        }
    }

    /**
     * Sorted intervals are pairwise disjoint exactly when every adjacent pair is, so one scan settles
     * the whole set — including an open-ended entry, whose unbounded end collides with its successor.
     */
    public static void requireNoOverlap(List<SubmittedDiscount> resulting) {
        List<SubmittedDiscount> ordered = resulting.stream()
                .sorted(Comparator.comparing(entry -> entry.discount().startsAt()))
                .toList();
        for (int i = 1; i < ordered.size(); i++) {
            SubmittedDiscount earlier = ordered.get(i - 1);
            SubmittedDiscount later = ordered.get(i);
            if (earlier.discount().period().overlaps(later.discount().period())) {
                throw overlapOf(later, earlier);
            }
        }
    }

    /** Validates a planned replacement: what changed, plus the overlap of everything that survives. */
    public static void validateReplacement(DiscountReplacement plan, BigDecimal productPrice, Instant now) {
        for (SubmittedDiscount entry : plan.created()) {
            validatePeriod(entry.index(), entry.discount().period());
            requireNotEntirelyPast(entry.index(), entry.discount().period(), now);
            validateValue(entry.index(), entry.discount().type(), entry.discount().value(), productPrice);
        }
        for (DiscountReplacement.Change change : plan.updated()) {
            if (change.periodChanged()) {
                requireStartUnchangedWhileActive(change.index(), change.stored(),
                        change.submitted().period(), now);
                validatePeriod(change.index(), change.submitted().period());
                requireNotEntirelyPast(change.index(), change.submitted().period(), now);
            }
            if (change.contentChanged()) {
                validateValue(change.index(), change.submitted().type(), change.submitted().value(),
                        productPrice);
            }
        }
        requireNoOverlap(plan.resulting());
    }

    /** Whether one more discount may join a product that already has a schedule. */
    public static Optional<SkipReason> checkAddition(DiscountSchedule schedule, Discount candidate,
                                                     BigDecimal productPrice, Instant now) {
        if (candidate.type() == DiscountType.AMOUNT && candidate.value().compareTo(productPrice) >= 0) {
            return Optional.of(SkipReason.AMOUNT_EXCEEDS_PRICE);
        }
        return schedule.conflictWith(candidate.period(), null).map(conflict -> SkipReason.PERIOD_OVERLAP);
    }

    /**
     * A new product price must stay above every fixed-amount discount that has not ended. Percent
     * discounts follow the price, and history is never rewritten.
     */
    public static void requireCompatibleWithPrice(DiscountSchedule schedule, BigDecimal newPrice, Instant now) {
        for (Discount discount : schedule.liveAt(now)) {
            if (discount.type() == DiscountType.AMOUNT && discount.value().compareTo(newPrice) >= 0) {
                throw new InvalidDiscountRequestException("price",
                        "New price is not above the amount discount " + discount.publicId()
                                + " (" + discount.value().stripTrailingZeros().toPlainString() + ")");
            }
        }
    }

    private static DiscountOverlapException overlapOf(SubmittedDiscount submitted, SubmittedDiscount conflicting) {
        Discount other = conflicting.discount();
        String describedEnd = other.endsAt() == null ? "open-ended" : other.endsAt().toString();
        String name = other.label() != null && !other.label().isBlank()
                ? "«" + other.label() + "»"
                : String.valueOf(other.publicId());
        return new DiscountOverlapException(
                fieldOf(submitted.index(), "starts_at"),
                "The period overlaps discount " + name + " (" + other.startsAt() + " – " + describedEnd + ")",
                sideOf(submitted),
                sideOf(conflicting));
    }

    private static DiscountOverlapException.Side sideOf(SubmittedDiscount entry) {
        Discount discount = entry.discount();
        return new DiscountOverlapException.Side(entry.index(), discount.publicId(),
                discount.startsAt(), discount.endsAt());
    }
}
