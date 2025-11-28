package com.erp.erp_back.util;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class DateRangeUtils {

    private DateRangeUtils() {}

    public record DateRange(LocalDateTime start, LocalDateTime end) {}

    public static DateRange between(LocalDate from, LocalDate toInclusive) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = toInclusive.plusDays(1).atStartOfDay();
        return new DateRange(start, end);
    }

    public static DateRange forToday() {
        LocalDate today = LocalDate.now();
        return between(today, today);
    }

    public static DateRange forThisWeek(LocalDate today) {
        LocalDate monday = today.minusDays(today.getDayOfWeek().getValue() - 1);
        LocalDate sunday = monday.plusDays(6);
        return between(monday, sunday);
    }

    public static DateRange forThisMonth(LocalDate today) {
        LocalDate first = today.withDayOfMonth(1);
        LocalDate last = today.withDayOfMonth(today.lengthOfMonth());
        return between(first, last);
    }

    public static DateRange forThisYear(LocalDate today) {
        LocalDate first = today.withDayOfYear(1);
        LocalDate last = today.withDayOfYear(today.lengthOfYear());
        return between(first, last);
    }
}
