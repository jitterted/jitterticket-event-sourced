package dev.ted.jitterticket.eventsourced.adapter.in.web;

import dev.ted.jitterticket.eventsourced.domain.ConcertId;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

public record ConcertView(
        String concertId,
        String artist,
        String ticketPrice,
        String showDate,
        String showTime) {
    static ConcertView create(ConcertId concertId,
                              String artist,
                              LocalDateTime showDateTime,
                              int ticketPrice) {
        String showDate = showDateTime
                .toLocalDate()
                .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG));
        String showTime = showDateTime
                .toLocalTime()
                .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT));
        String ticketPriceString = "$" + ticketPrice;

        return new ConcertView(concertId.id().toString(),
                               artist,
                               ticketPriceString,
                               showDate,
                               showTime);
    }
}
