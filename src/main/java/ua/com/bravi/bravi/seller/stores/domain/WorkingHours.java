package ua.com.bravi.bravi.seller.stores.domain;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public record WorkingHours(
        DayInterval monday,
        DayInterval tuesday,
        DayInterval wednesday,
        DayInterval thursday,
        DayInterval friday,
        DayInterval saturday,
        DayInterval sunday
) {
     public record DayInterval(
              @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm") LocalTime from,
              @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm") LocalTime to,
              Boolean closed
     ) {}

    public boolean isOpenAt(ZoneId zone, Instant instant) {
        ZonedDateTime local = instant.atZone(zone);
        DayInterval interval = intervalFor(local.getDayOfWeek());
        if (interval == null || interval.from() == null || interval.to() == null) {
            return false;
        }
        LocalTime time = local.toLocalTime();
        return !time.isBefore(interval.from()) && time.isBefore(interval.to());
    }

    private DayInterval intervalFor(DayOfWeek day) {
        return switch (day) {
            case MONDAY -> monday;
            case TUESDAY -> tuesday;
            case WEDNESDAY -> wednesday;
            case THURSDAY -> thursday;
            case FRIDAY -> friday;
            case SATURDAY -> saturday;
            case SUNDAY -> sunday;
        };
    }
}
