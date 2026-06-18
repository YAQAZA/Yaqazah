package com.yaqazah.dashboard.util;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public final class DashboardFilterResolver {

    public record DateRange(LocalDate from, LocalDate to) {
    }

    private DashboardFilterResolver() {
    }

    public static String toFilterId(String filter) {
        return switch (normalize(filter)) {
            case "all" -> "0";
            case "today" -> "1";
            case "yesterday" -> "2";
            case "lastweek" -> "3";
            case "lastmonth" -> "4";
            case "lastyear" -> "5";
            case "custom" -> "6";
            default -> throw new IllegalArgumentException("Unknown filter: " + filter);
        };
    }

    public static DateRange resolve(String filter, String fromIso, String toIso) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        return switch (normalize(filter)) {
            case "all" -> new DateRange(LocalDate.of(2000, 1, 1), today);
            case "today" -> new DateRange(today, today);
            case "yesterday" -> {
                LocalDate yesterday = today.minusDays(1);
                yield new DateRange(yesterday, yesterday);
            }
            case "lastweek" -> new DateRange(today.minusDays(6), today);
            case "lastmonth" -> new DateRange(today.minusDays(29), today);
            case "lastyear" -> new DateRange(today.minusDays(364), today);
            case "custom" -> resolveCustom(fromIso, toIso);
            default -> throw new IllegalArgumentException("Unknown filter: " + filter);
        };
    }

    private static DateRange resolveCustom(String fromIso, String toIso) {
        if (fromIso == null || fromIso.isBlank() || toIso == null || toIso.isBlank()) {
            throw new IllegalArgumentException("'from' and 'to' are required when filter is custom");
        }
        LocalDate from = parseIsoDate(fromIso);
        LocalDate to = parseIsoDate(toIso);
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("'to' must be on or after 'from'");
        }
        return new DateRange(from, to);
    }

    private static LocalDate parseIsoDate(String iso) {
        try {
            if (iso.length() > 10) {
                return java.time.Instant.parse(iso).atZone(ZoneOffset.UTC).toLocalDate();
            }
            return LocalDate.parse(iso, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Invalid ISO 8601 date: " + iso);
        }
    }

    private static String normalize(String filter) {
        if (filter == null || filter.isBlank()) {
            throw new IllegalArgumentException("filter is required");
        }
        return filter.trim().toLowerCase();
    }
}
