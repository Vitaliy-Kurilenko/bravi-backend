package ua.com.bravi.bravi.seller.catalog.discounts.domain;

import ua.com.bravi.bravi.seller.catalog.discounts.exception.InvalidDiscountRequestException;
import ua.com.bravi.bravi.shared.exception.NotFoundException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * What a submitted schedule means for the stored one. An entry carrying a public id keeps that row and
 * its creation time, an entry without one is new, and a stored row nobody resubmitted is dropped —
 * which is how a running discount is stopped early.
 */
public record DiscountReplacement(
        List<SubmittedDiscount> created,
        List<Change> updated,
        List<Discount> deleted,
        List<SubmittedDiscount> resulting) {

    /** A stored row and the content submitted for it. */
    public record Change(int index, Discount stored, Discount submitted) {

        public boolean periodChanged() {
            return !stored.period().sameAs(submitted.period());
        }

        /** Only type and value matter here: they are what the value rules are checked against. */
        public boolean contentChanged() {
            return stored.type() != submitted.type() || stored.value().compareTo(submitted.value()) != 0;
        }

        public boolean changed() {
            return periodChanged() || contentChanged() || !Objects.equals(stored.label(), submitted.label());
        }

        /**
         * The stored row carrying the submitted content, keeping identity and creation time. A row
         * resubmitted unchanged keeps its modification time too, so saving a schedule nobody edited
         * does not make every discount look freshly touched.
         */
        public Discount merged(Instant at) {
            return new Discount(stored.id(), stored.publicId(), stored.productId(),
                    submitted.type(), submitted.value(), submitted.startsAt(), submitted.endsAt(),
                    submitted.label(), stored.createdAt(), changed() ? at : stored.updatedAt());
        }
    }

    /**
     * @throws NotFoundException               an entry names a public id this product does not carry
     * @throws InvalidDiscountRequestException the same public id is submitted more than once
     */
    public static DiscountReplacement plan(List<Discount> stored, List<SubmittedDiscount> submitted,
                                          Instant at) {
        Map<String, Discount> storedByPublicId = new LinkedHashMap<>();
        stored.forEach(discount -> storedByPublicId.put(discount.publicId(), discount));

        List<SubmittedDiscount> created = new ArrayList<>();
        List<Change> updated = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        for (SubmittedDiscount entry : submitted) {
            String publicId = entry.discount().publicId();
            if (publicId == null) {
                created.add(entry);
                continue;
            }
            if (!seen.add(publicId)) {
                throw new InvalidDiscountRequestException(
                        DiscountPolicy.fieldOf(entry.index(), "public_id"),
                        "Discount is submitted more than once");
            }
            Discount match = storedByPublicId.get(publicId);
            if (match == null) {
                throw new NotFoundException("Discount not found");
            }
            updated.add(new Change(entry.index(), match, entry.discount()));
        }

        List<Discount> deleted = stored.stream()
                .filter(discount -> !seen.contains(discount.publicId()))
                .toList();

        List<SubmittedDiscount> resulting = new ArrayList<>();
        updated.forEach(change -> resulting.add(new SubmittedDiscount(change.index(), change.merged(at))));
        resulting.addAll(created);

        return new DiscountReplacement(List.copyOf(created), List.copyOf(updated), deleted,
                List.copyOf(resulting));
    }
}
