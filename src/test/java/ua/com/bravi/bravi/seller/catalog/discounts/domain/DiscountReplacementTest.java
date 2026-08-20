package ua.com.bravi.bravi.seller.catalog.discounts.domain;

import org.junit.jupiter.api.Test;
import ua.com.bravi.bravi.seller.catalog.discounts.exception.InvalidDiscountRequestException;
import ua.com.bravi.bravi.shared.exception.NotFoundException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DiscountReplacementTest {

    private static final BigDecimal PRICE = new BigDecimal("1200.0000");
    private static final Instant NOW = Instant.parse("2026-06-01T00:00:00Z");
    private static final Instant CREATED = Instant.parse("2025-12-01T00:00:00Z");
    private static final Instant TOUCHED = Instant.parse("2026-01-15T00:00:00Z");

    private static Instant at(String iso) {
        return Instant.parse(iso);
    }

    private static Discount stored(Long id, String publicId, String value, Instant from, Instant to) {
        return new Discount(id, publicId, 7L, DiscountType.PERCENT, new BigDecimal(value),
                from, to, "kept", CREATED, TOUCHED);
    }

    private static SubmittedDiscount submitted(int index, String publicId, String value, Instant from, Instant to) {
        return new SubmittedDiscount(index, new Discount(null, publicId, null, DiscountType.PERCENT,
                new BigDecimal(value), from, to, "kept", null, null));
    }

    private static DiscountReplacement plan(List<Discount> stored, SubmittedDiscount... submitted) {
        return DiscountReplacement.plan(stored, List.of(submitted), NOW);
    }

    @Test
    void entriesAreSplitIntoCreatedUpdatedAndDeleted() {
        Discount keep = stored(1L, "dsc_keep", "20", at("2026-05-01T00:00:00Z"), at("2026-07-01T00:00:00Z"));
        Discount drop = stored(2L, "dsc_drop", "10", at("2026-08-01T00:00:00Z"), at("2026-09-01T00:00:00Z"));

        DiscountReplacement result = plan(List.of(keep, drop),
                submitted(0, "dsc_keep", "20", at("2026-05-01T00:00:00Z"), at("2026-07-01T00:00:00Z")),
                submitted(1, null, "15", at("2026-10-01T00:00:00Z"), null));

        assertThat(result.updated()).singleElement()
                .satisfies(change -> assertThat(change.stored().publicId()).isEqualTo("dsc_keep"));
        assertThat(result.created()).singleElement()
                .satisfies(entry -> assertThat(entry.index()).isEqualTo(1));
        assertThat(result.deleted()).singleElement()
                .satisfies(discount -> assertThat(discount.publicId()).isEqualTo("dsc_drop"));
        assertThat(result.resulting()).hasSize(2);
    }

    @Test
    void anUpdatedRowKeepsItsIdentityAndCreationTime() {
        Discount keep = stored(1L, "dsc_keep", "20", at("2026-08-01T00:00:00Z"), at("2026-09-01T00:00:00Z"));

        DiscountReplacement result = plan(List.of(keep),
                submitted(0, "dsc_keep", "25", at("2026-08-01T00:00:00Z"), at("2026-09-01T00:00:00Z")));

        Discount merged = result.updated().getFirst().merged(NOW);
        assertThat(merged.id()).isEqualTo(1L);
        assertThat(merged.publicId()).isEqualTo("dsc_keep");
        assertThat(merged.createdAt()).isEqualTo(CREATED);
        assertThat(merged.value()).isEqualByComparingTo("25");
        assertThat(merged.updatedAt()).isEqualTo(NOW);
    }

    @Test
    void aRowResubmittedUnchangedKeepsItsModificationTime() {
        Discount keep = stored(1L, "dsc_keep", "20", at("2026-08-01T00:00:00Z"), at("2026-09-01T00:00:00Z"));

        DiscountReplacement result = plan(List.of(keep),
                submitted(0, "dsc_keep", "20", at("2026-08-01T00:00:00Z"), at("2026-09-01T00:00:00Z")));

        assertThat(result.updated().getFirst().changed()).isFalse();
        assertThat(result.updated().getFirst().merged(NOW).updatedAt()).isEqualTo(TOUCHED);
    }

    @Test
    void resubmittingAnExpiredDiscountVerbatimIsAccepted() {
        // The rule forbidding a period entirely in the past has to apply to new entries only, or saving
        // an untouched schedule would fail as soon as one of its discounts had run its course.
        Discount expired = stored(1L, "dsc_old", "20", at("2026-01-01T00:00:00Z"), at("2026-02-01T00:00:00Z"));

        DiscountReplacement result = plan(List.of(expired),
                submitted(0, "dsc_old", "20", at("2026-01-01T00:00:00Z"), at("2026-02-01T00:00:00Z")));

        assertThat(result.updated().getFirst().periodChanged()).isFalse();
        assertThatCode(() -> DiscountPolicy.validateReplacement(result, PRICE, NOW)).doesNotThrowAnyException();
    }

    @Test
    void editingAnExpiredDiscountStillInThePastIsRejected() {
        Discount expired = stored(1L, "dsc_old", "20", at("2026-01-01T00:00:00Z"), at("2026-02-01T00:00:00Z"));

        DiscountReplacement result = plan(List.of(expired),
                submitted(0, "dsc_old", "20", at("2026-01-01T00:00:00Z"), at("2026-03-01T00:00:00Z")));

        assertThatThrownBy(() -> DiscountPolicy.validateReplacement(result, PRICE, NOW))
                .isInstanceOf(InvalidDiscountRequestException.class)
                .hasMessage("The period is entirely in the past");
    }

    @Test
    void removingARunningDiscountIsAllowedBecauseItIsHowYouStopItEarly() {
        Discount running = stored(1L, "dsc_live", "20", at("2026-05-01T00:00:00Z"), at("2026-07-01T00:00:00Z"));

        DiscountReplacement result = plan(List.of(running));

        assertThat(result.deleted()).singleElement()
                .satisfies(discount -> assertThat(discount.publicId()).isEqualTo("dsc_live"));
        assertThatCode(() -> DiscountPolicy.validateReplacement(result, PRICE, NOW)).doesNotThrowAnyException();
    }

    @Test
    void movingTheStartOfARunningDiscountIsRejected() {
        Discount running = stored(1L, "dsc_live", "20", at("2026-05-01T00:00:00Z"), at("2026-07-01T00:00:00Z"));

        DiscountReplacement result = plan(List.of(running),
                submitted(0, "dsc_live", "20", at("2026-05-15T00:00:00Z"), at("2026-07-01T00:00:00Z")));

        assertThatThrownBy(() -> DiscountPolicy.validateReplacement(result, PRICE, NOW))
                .isInstanceOf(InvalidDiscountRequestException.class)
                .hasMessage("The start of a running discount cannot be moved");
    }

    @Test
    void changingTheValueOfARunningDiscountIsAllowed() {
        Discount running = stored(1L, "dsc_live", "20", at("2026-05-01T00:00:00Z"), at("2026-07-01T00:00:00Z"));

        DiscountReplacement result = plan(List.of(running),
                submitted(0, "dsc_live", "30", at("2026-05-01T00:00:00Z"), at("2026-07-01T00:00:00Z")));

        assertThatCode(() -> DiscountPolicy.validateReplacement(result, PRICE, NOW)).doesNotThrowAnyException();
    }

    @Test
    void swappingTwoPeriodsInOneSubmissionIsAccepted() {
        Discount first = stored(1L, "dsc_a", "20", at("2026-08-01T00:00:00Z"), at("2026-09-01T00:00:00Z"));
        Discount second = stored(2L, "dsc_b", "10", at("2026-09-01T00:00:00Z"), at("2026-10-01T00:00:00Z"));

        DiscountReplacement result = plan(List.of(first, second),
                submitted(0, "dsc_a", "20", at("2026-09-01T00:00:00Z"), at("2026-10-01T00:00:00Z")),
                submitted(1, "dsc_b", "10", at("2026-08-01T00:00:00Z"), at("2026-09-01T00:00:00Z")));

        assertThatCode(() -> DiscountPolicy.validateReplacement(result, PRICE, NOW)).doesNotThrowAnyException();
    }

    @Test
    void anUnknownPublicIdIsNotFound() {
        assertThatThrownBy(() -> plan(List.of(),
                submitted(0, "dsc_missing", "20", at("2026-08-01T00:00:00Z"), null)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void theSamePublicIdSubmittedTwiceIsRejected() {
        Discount keep = stored(1L, "dsc_keep", "20", at("2026-08-01T00:00:00Z"), at("2026-09-01T00:00:00Z"));

        assertThatThrownBy(() -> plan(List.of(keep),
                submitted(0, "dsc_keep", "20", at("2026-08-01T00:00:00Z"), at("2026-09-01T00:00:00Z")),
                submitted(1, "dsc_keep", "10", at("2026-10-01T00:00:00Z"), null)))
                .isInstanceOfSatisfying(InvalidDiscountRequestException.class,
                        ex -> assertThat(ex.getField()).isEqualTo("discounts[1].public_id"));
    }
}
