package dev.ted.jitterticket.eventsourced.adapter.in.web;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class LocalDateTimeFormatting {

    // Date format for browsers <input type="date"> tag is YYYY-MM-DD -- dashes only! (not slash separators)
    // Time format for the browser's <input type="time"> tag is HH:MM in 24 hour format
    public static final DateTimeFormatter YYYY_MM_DD_HH_MM_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    public static final DateTimeFormatter YYYY_MM_DD = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    public static LocalDateTime fromBrowserDateAndTime(String rawDate, String rawTime) {
        return LocalDateTime.parse(rawDate + " " + rawTime, YYYY_MM_DD_HH_MM_FORMATTER);
    }

    /**
     * Format Local date time as used by browser's Date parsing function Date.parse()
     * and elsewhere, such as in JSON
     *
     * @param localDateTime date time, usually in UTC (time zone "Z")
     * @return String formatted for use in JavaScript
     */
    public static String formatAsDateTimeForCommonIso8601(LocalDateTime localDateTime) {
        return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(localDateTime);
    }

    public static String extractFormattedTimeFrom(LocalDateTime localDateTime) {
        return localDateTime.format(HH_MM);
    }

    public static String formatAsTimeFrom(LocalTime localTime) {
        return localTime.format(HH_MM);
    }

    public static String extractFormattedDateFrom(LocalDateTime localDateTime) {
        return localDateTime.format(YYYY_MM_DD);
    }
}