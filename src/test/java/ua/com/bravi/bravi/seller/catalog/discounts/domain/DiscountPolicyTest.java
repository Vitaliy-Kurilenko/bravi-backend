package ua.com.bravi.bravi.seller.catalog.discounts.domain;

import org.junit.jupiter.api.Test;
import ua.com.bravi.bravi.seller.catalog.discounts.exception.DiscountOverlapException;
import ua.com.bravi.bravi.seller.catalog.discounts.exception.InvalidDiscountRequestException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DiscountPolicyTest {

    private static final BigDecimal PRICE = new BigDecimal("1200.0000");
    private static final Instant NOW = Instant.parse("2026-06-01T00:00:00Z");

    private static Discount discount(String value, Instant from, Instant to) {
        return new Discount(null, null, 1L, DiscountType.PERCENT, new BigDecimal(value),
                from, to, null, null, null);
    }

    private static Instant at(String iso) {
        return Instant.parse(iso);
    }

    @Test
    void percentOutsideTheRangeIsRejected() {
        for (String value : new String[]{"0", "0.009", "99.01", "100"}) {
            assertThatThrownBy(() -> DiscountPolicy.validateValue(0, DiscountType.PERCENT, new BigDecimal(value), PRICE))
                    .as("percent %s", value)
                    .isInstanceOf(InvalidDiscountRequestException.class)
                    .hasMessage("Percent discount must be between 0.01 and 99");
        }
    }

    @Test
    void percentAtBothEndsOfTheRangeIsAccepted() {
        assertThatCode(() -> DiscountPolicy.validateValue(0, DiscountType.PERCENT, new BigDecimal("0.01"), PRICE))
                .doesNotThrowAnyException();
        assertThatCode(() -> DiscountPolicy.validateValue(0, DiscountType.PERCENT, new BigDecimal("99"), PRICE))
                .doesNotThrowAnyException();
    }

    @Test
    void amountAtOrAboveThePriceIsRejectedAndJustBelowIsAccepted() {
        assertThatThrownBy(() -> DiscountPolicy.validateValue(0, DiscountType.AMOUNT, PRICE, PRICE))
                .isInstanceOf(InvalidDiscountRequestException.class)
                .hasMessageContaining("larger than the product price");
        assertThatCode(() -> DiscountPolicy.validateValue(0, DiscountType.AMOUNT, new BigDecimal("1199.99"), PRICE))
                .doesNotThrowAnyException();
    }

    @Test
    void validationErrorsAddressTheSubmittedEntry() {
        assertThatThrownBy(() -> DiscountPolicy.validateValue(2, DiscountType.PERCENT, new BigDecimal("100"), PRICE))
                .isInstanceOfSatisfying(InvalidDiscountRequestException.class,
                        ex -> assertThat(ex.getField()).isEqualTo("discounts[2].value"));
    }

    @Test
    void endBeforeOrEqualToTheStartIsRejected() {
        assertThatThrownBy(() -> DiscountPolicy.validatePeriod(0,
                new DiscountPeriod(at("2026-07-01T00:00:00Z"), at("2026-07-01T00:00:00Z"))))
                .isInstanceOf(InvalidDiscountRequestException.class)
                .hasMessage("End of the period must be after its start");
    }

    @Test
    void aPeriodEntirelyInThePastIsRejectedButAStartedRunningOneIsNot() {
        assertThatThrownBy(() -> DiscountPolicy.requireNotEntirelyPast(0,
                new DiscountPeriod(at("2026-01-01T00:00:00Z"), at("2026-02-01T00:00:00Z")), NOW))
                .isInstanceOf(InvalidDiscountRequestException.class)
                .hasMessage("The period is entirely in the past");

        assertThatCode(() -> DiscountPolicy.requireNotEntirelyPast(0,
                new DiscountPeriod(at("2026-01-01T00:00:00Z"), at("2026-12-01T00:00:00Z")), NOW))
                .doesNotThrowAnyException();
    }

    @Test
    void aShortPeriodIsAcceptedBecauseTheHourWarningBelongsToTheClient() {
        assertThatCode(() -> DiscountPolicy.validatePeriod(0,
                new DiscountPeriod(at("2026-07-01T00:00:00Z"), at("2026-07-01T00:40:00Z"))))
                .doesNotThrowAnyException();
    }

    @Test
    void theStartOfARunningDiscountCannotBeMoved() {
        Discount stored = discount("20", at("2026-05-01T00:00:00Z"), at("2026-07-01T00:00:00Z"));

        assertThatThrownBy(() -> DiscountPolicy.requireStartUnchangedWhileActive(1, stored,
                new DiscountPeriod(at("2026-05-02T00:00:00Z"), at("2026-07-01T00:00:00Z")), NOW))
                .isInstanceOfSatisfying(InvalidDiscountRequestException.class,
                        ex -> assertThat(ex.getField()).isEqualTo("discounts[1].starts_at"));
    }

    @Test
    void theStartOfAScheduledDiscountMayBeMoved() {
        Discount stored = discount("20", at("2026-08-01T00:00:00Z"), at("2026-09-01T00:00:00Z"));

        assertThatCode(() -> DiscountPolicy.requireStartUnchangedWhileActive(0, stored,
                new DiscountPeriod(at("2026-08-15T00:00:00Z"), at("2026-09-01T00:00:00Z")), NOW))
                .doesNotThrowAnyException();
    }

    @Test
    void overlapIsReportedWithBothSides() {
        List<SubmittedDiscount> resulting = List.of(
                new SubmittedDiscount(0, discount("20", at("2026-07-01T00:00:00Z"), at("2026-08-01T00:00:00Z"))),
                new SubmittedDiscount(2, discount("10", at("2026-07-15T00:00:00Z"), at("2026-09-01T00:00:00Z"))));

        assertThatThrownBy(() -> DiscountPolicy.requireNoOverlap(resulting))
                .isInstanceOfSatisfying(DiscountOverlapException.class, ex -> {
                    assertThat(ex.getField()).isEqualTo("discounts[2].starts_at");
                    assertThat(ex.getSubmitted().index()).isEqualTo(2);
                    assertThat(ex.getConflicting().index()).isZero();
                });
    }

    @Test
    void anOpenEndedEntryConflictsWithALaterScheduledOne() {
        List<SubmittedDiscount> resulting = List.of(
                new SubmittedDiscount(0, discount("5", at("2026-07-01T00:00:00Z"), null)),
                new SubmittedDiscount(1, discount("10", at("2026-12-24T00:00:00Z"), at("2027-01-07T00:00:00Z"))));

        assertThatThrownBy(() -> DiscountPolicy.requireNoOverlap(resulting))
                .isInstanceOf(DiscountOverlapException.class);
    }

    @Test
    void abuttingEntriesAreAccepted() {
        List<SubmittedDiscount> resulting = List.of(
                new SubmittedDiscount(0, discount("20", at("2026-07-01T00:00:00Z"), at("2026-08-01T00:00:00Z"))),
                new SubmittedDiscount(1, discount("10", at("2026-08-01T00:00:00Z"), null)));

        assertThatCode(() -> DiscountPolicy.requireNoOverlap(resulting)).doesNotThrowAnyException();
    }

    @Test
    void aPriceChangeMustStayAboveEveryLiveAmountDiscount() {
        Discount live = new Discount(1L, "dsc_live", 1L, DiscountType.AMOUNT, new BigDecimal("200"),
                at("2026-05-01T00:00:00Z"), at("2026-07-01T00:00:00Z"), null, null, null);
        DiscountSchedule schedule = DiscountSchedule.of(List.of(live));

        assertThatThrownBy(() -> DiscountPolicy.requireCompatibleWithPrice(schedule, new BigDecimal("150"), NOW))
                .isInstanceOfSatisfying(InvalidDiscountRequestException.class,
                        ex -> assertThat(ex.getField()).isEqualTo("price"));
        assertThatCode(() -> DiscountPolicy.requireCompatibleWithPrice(schedule, new BigDecimal("250"), NOW))
                .doesNotThrowAnyException();
    }

    @Test
    void aPriceChangeIgnoresExpiredAndPercentDiscounts() {
        Discount expired = new Discount(1L, "dsc_old", 1L, DiscountType.AMOUNT, new BigDecimal("200"),
                at("2026-01-01T00:00:00Z"), at("2026-02-01T00:00:00Z"), null, null, null);
        Discount percent = new Discount(2L, "dsc_pct", 1L, DiscountType.PERCENT, new BigDecimal("90"),
                at("2026-05-01T00:00:00Z"), null, null, null, null);

        assertThatCode(() -> DiscountPolicy.requireCompatibleWithPrice(
                DiscountSchedule.of(List.of(expired, percent)), new BigDecimal("10"), NOW))
                .doesNotThrowAnyException();
    }

    @Test
    void bulkAdditionReportsWhyAProductIsSkipped() {
        Discount stored = new Discount(1L, "dsc_1", 1L, DiscountType.PERCENT, new BigDecimal("20"),
                at("2026-05-01T00:00:00Z"), at("2026-07-01T00:00:00Z"), null, null, null);
        DiscountSchedule schedule = DiscountSchedule.of(List.of(stored));

        assertThat(DiscountPolicy.checkAddition(schedule,
                discount("10", at("2026-06-01T00:00:00Z"), at("2026-06-15T00:00:00Z")), PRICE, NOW))
                .contains(SkipReason.PERIOD_OVERLAP);

        Discount tooBig = new Discount(null, null, 1L, DiscountType.AMOUNT, new BigDecimal("5000"),
                at("2026-08-01T00:00:00Z"), at("2026-09-01T00:00:00Z"), null, null, null);
        assertThat(DiscountPolicy.checkAddition(schedule, tooBig, PRICE, NOW))
                .contains(SkipReason.AMOUNT_EXCEEDS_PRICE);

        assertThat(DiscountPolicy.checkAddition(schedule,
                discount("10", at("2026-08-01T00:00:00Z"), at("2026-09-01T00:00:00Z")), PRICE, NOW))
                .isEmpty();
    }
}
