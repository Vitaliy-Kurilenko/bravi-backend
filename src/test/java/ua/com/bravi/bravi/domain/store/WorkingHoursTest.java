package ua.com.bravi.bravi.domain.store;

import org.junit.jupiter.api.Test;
import ua.com.bravi.bravi.domain.store.WorkingHours.DayInterval;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class WorkingHoursTest {

    private static final ZoneId KYIV = ZoneId.of("Europe/Kyiv");

    private static final DayInterval NINE_TO_SIX =
            new DayInterval(LocalTime.of(9, 0), LocalTime.of(18, 0), false);

    @Test
    void openInsideInterval() {
        WorkingHours hours = workdayOnly(NINE_TO_SIX);

        // Monday, 2026-03-02 at 10:30 Kyiv
        var instant = ZonedDateTime.of(LocalDateTime.of(2026, 3, 2, 10, 30), KYIV).toInstant();

        assertThat(hours.isOpenAt(KYIV, instant)).isTrue();
    }

    @Test
    void openAtExactFromBoundary() {
        WorkingHours hours = workdayOnly(NINE_TO_SIX);

        var instant = ZonedDateTime.of(LocalDateTime.of(2026, 3, 2, 9, 0), KYIV).toInstant();

        assertThat(hours.isOpenAt(KYIV, instant)).isTrue();
    }

    @Test
    void closedAtExactToBoundary() {
        WorkingHours hours = workdayOnly(NINE_TO_SIX);

        var instant = ZonedDateTime.of(LocalDateTime.of(2026, 3, 2, 18, 0), KYIV).toInstant();

        assertThat(hours.isOpenAt(KYIV, instant)).isFalse();
    }

    @Test
    void closedBeforeIntervalStarts() {
        WorkingHours hours = workdayOnly(NINE_TO_SIX);

        var instant = ZonedDateTime.of(LocalDateTime.of(2026, 3, 2, 8, 59), KYIV).toInstant();

        assertThat(hours.isOpenAt(KYIV, instant)).isFalse();
    }

    @Test
    void closedWhenDayIntervalIsNull() {
        // Sunday interval null -> closed
        WorkingHours hours = new WorkingHours(
                NINE_TO_SIX, NINE_TO_SIX, NINE_TO_SIX,
                NINE_TO_SIX, NINE_TO_SIX, NINE_TO_SIX, null
        );

        // Sunday, 2026-03-01 at noon Kyiv
        var instant = ZonedDateTime.of(LocalDateTime.of(2026, 3, 1, 12, 0), KYIV).toInstant();

        assertThat(hours.isOpenAt(KYIV, instant)).isFalse();
    }

    @Test
    void closedWhenFromOrToIsNull() {
        DayInterval missingTo = new DayInterval(LocalTime.of(9, 0), null, false);
        WorkingHours hours = workdayOnly(missingTo);

        var instant = ZonedDateTime.of(LocalDateTime.of(2026, 3, 2, 12, 0), KYIV).toInstant();

        assertThat(hours.isOpenAt(KYIV, instant)).isFalse();
    }

    @Test
    void dayResolutionRespectsTimezone() {
        WorkingHours hours = new WorkingHours(
                NINE_TO_SIX, null, null, null, null, null, null
        );

        // Tuesday 00:30 UTC == Tuesday 02:30 Kyiv (before interval, but for Tuesday — Tuesday is null)
        var tuesdayKyiv = ZonedDateTime.of(LocalDateTime.of(2026, 3, 3, 2, 30), KYIV).toInstant();
        assertThat(hours.isOpenAt(KYIV, tuesdayKyiv)).isFalse();

        // 23:30 UTC on Monday == 01:30 Kyiv Tuesday — Tuesday is null
        var stillTuesdayKyiv = ZonedDateTime.of(LocalDateTime.of(2026, 3, 2, 23, 30), ZoneId.of("UTC")).toInstant();
        assertThat(hours.isOpenAt(KYIV, stillTuesdayKyiv)).isFalse();
    }

    private static WorkingHours workdayOnly(DayInterval interval) {
        return new WorkingHours(
                interval, interval, interval, interval, interval, null, null
        );
    }
}
