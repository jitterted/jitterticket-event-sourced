package dev.ted.jitterticket.eventsourced.adapter.in.web;

import dev.ted.jitterticket.eventsourced.application.port.EventStore;
import dev.ted.jitterticket.eventsourced.domain.concert.Concert;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.UUID;

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
        String showDate = showDateTime.toLocalDate()
                .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG));
        String showTime = showDateTime.toLocalTime()
                .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT));
        String ticketPriceString = "$" + ticketPrice;

        return new ConcertView(concertId.id().toString(),
                               artist,
                               ticketPriceString,
                               showDate,
                               showTime);
    }

    static ConcertView from(Concert concert) {
        return create(concert.getId(),
                      concert.artist(),
                      concert.showDateTime(),
                      concert.ticketPrice()
        );
    }

    static ConcertView concertViewFor(EventStore<ConcertId, ConcertEvent, Concert> concertStore, String concertId) {
        return concertStore.findById(new ConcertId(UUID.fromString(concertId)))
                           .map(ConcertView::from)
                           .orElseThrow(() -> new RuntimeException("Could not find concert with id: " + concertId));
    }
}
