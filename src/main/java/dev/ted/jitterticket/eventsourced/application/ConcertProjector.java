package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.domain.Concert;
import dev.ted.jitterticket.eventsourced.domain.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.ConcertId;
import dev.ted.jitterticket.eventsourced.domain.ConcertRescheduled;
import dev.ted.jitterticket.eventsourced.domain.ConcertScheduled;

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

    public Stream<ConcertTicketView> allConcertTicketViews() {
        Map<ConcertId, ConcertTicketView> views = new HashMap<>();
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
                                                    new ConcertTicketView(concertId, artist, ticketPrice, showDateTime, doorsTime));

                                     case ConcertRescheduled(ConcertId concertId, LocalDateTime newShowDateTime, LocalTime newDoorsTime) -> {
                                         ConcertTicketView oldView = views.get(concertId);
                                         ConcertTicketView rescheduledView = rescheduleTo(newShowDateTime, newDoorsTime, oldView);
                                         views.put(concertId, rescheduledView);
                                     }

                                     default -> {
                                     }
                                 }
                             }
                    );
        return views.values().stream();
    }

    private ConcertTicketView rescheduleTo(LocalDateTime newShowDateTime, LocalTime newDoorsTime, ConcertTicketView oldView) {
        return new ConcertTicketView(
                oldView.concertId(),
                oldView.artist(),
                oldView.ticketPrice(),
                newShowDateTime,
                newDoorsTime
        );
    }
}

