package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.domain.concert.Concert;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertRescheduled;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertScheduled;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class ConcertProjector {

    private final EventStore<ConcertId, ConcertEvent, Concert> concertStore;

    public ConcertProjector(EventStore<ConcertId, ConcertEvent, Concert> concertStore) {
        this.concertStore = concertStore;
    }

    public Stream<ConcertSummary> allConcertTicketViews() {
        Map<ConcertId, ConcertSummary> views = new HashMap<>();
        concertStore.allEvents()
                    .forEach(concertEvent -> {
                                 switch (concertEvent) {
                                     case ConcertScheduled(
                                             ConcertId concertId,
                                             String artist,
                                             int ticketPrice,
                                             LocalDateTime showDateTime,
                                             LocalTime doorsTime,
                                             _, _) ->
                                             views.put(concertId,
                                                    new ConcertSummary(concertId, artist, ticketPrice, showDateTime, doorsTime));

                                     case ConcertRescheduled(ConcertId concertId, LocalDateTime newShowDateTime, LocalTime newDoorsTime) -> {
                                         ConcertSummary oldView = views.get(concertId);
                                         ConcertSummary rescheduledView = rescheduleTo(newShowDateTime, newDoorsTime, oldView);
                                         views.put(concertId, rescheduledView);
                                     }

                                     default -> {
                                     }
                                 }
                             }
                    );
        return views.values().stream();
    }

    private ConcertSummary rescheduleTo(LocalDateTime newShowDateTime, LocalTime newDoorsTime, ConcertSummary oldView) {
        return new ConcertSummary(
                oldView.concertId(),
                oldView.artist(),
                oldView.ticketPrice(),
                newShowDateTime,
                newDoorsTime
        );
    }
}

