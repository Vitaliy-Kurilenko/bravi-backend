package ua.com.bravi.bravi.seller.catalog.discounts.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class DiscountPeriodTest {

    private static final Instant JAN = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant FEB = Instant.parse("2026-02-01T00:00:00Z");
    private static final Instant MAR = Instant.parse("2026-03-01T00:00:00Z");
    private static final Instant APR = Instant.parse("2026-04-01T00:00:00Z");

    private static DiscountPeriod period(Instant from, Instant to) {
        return new DiscountPeriod(from, to);
    }

    @Test
    void disjointPeriodsDoNotOverlap() {
        assertThat(period(JAN, FEB).overlaps(period(MAR, APR))).isFalse();
    }

    @Test
    void touchingPeriodsDoNotOverlapBecauseTheWindowIsHalfOpen() {
        assertThat(period(JAN, FEB).overlaps(period(FEB, MAR))).isFalse();
        assertThat(period(FEB, MAR).overlaps(period(JAN, FEB))).isFalse();
    }

    @Test
    void crossingAndNestedPeriodsOverlap() {
        assertThat(period(JAN, MAR).overlaps(period(FEB, APR))).isTrue();
        assertThat(period(JAN, APR).overlaps(period(FEB, MAR))).isTrue();
        assertThat(period(JAN, MAR).overlaps(period(JAN, MAR))).isTrue();
    }

    @Test
    void openEndedPeriodOverlapsEverythingStartingAfterIt() {
        assertThat(period(JAN, null).overlaps(period(MAR, APR))).isTrue();
        assertThat(period(MAR, APR).overlaps(period(JAN, null))).isTrue();
    }

    @Test
    void openEndedPeriodDoesNotOverlapAnEarlierClosedOne() {
        assertThat(period(MAR, null).overlaps(period(JAN, FEB))).isFalse();
    }

    @Test
    void twoOpenEndedPeriodsAlwaysOverlap() {
        assertThat(period(JAN, null).overlaps(period(MAR, null))).isTrue();
    }

    @Test
    void statusIsScheduledBeforeTheStart() {
        assertThat(period(FEB, MAR).statusAt(JAN)).isEqualTo(DiscountStatus.SCHEDULED);
    }

    @Test
    void statusIsActiveExactlyOnTheStartAndExpiredExactlyOnTheEnd() {
        assertThat(period(FEB, MAR).statusAt(FEB)).isEqualTo(DiscountStatus.ACTIVE);
        assertThat(period(FEB, MAR).statusAt(MAR)).isEqualTo(DiscountStatus.EXPIRED);
    }

    @Test
    void openEndedPeriodIsNeverExpired() {
        assertThat(period(JAN, null).statusAt(APR)).isEqualTo(DiscountStatus.ACTIVE);
        assertThat(period(JAN, null).entirelyBefore(APR)).isFalse();
    }

    @Test
    void entirelyBeforeIsTrueOnceThePeriodHasEnded() {
        assertThat(period(JAN, FEB).entirelyBefore(MAR)).isTrue();
        assertThat(period(JAN, FEB).entirelyBefore(FEB)).isTrue();
        assertThat(period(JAN, MAR).entirelyBefore(FEB)).isFalse();
    }
}
