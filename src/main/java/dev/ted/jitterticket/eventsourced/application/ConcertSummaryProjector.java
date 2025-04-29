package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.domain.concert.Concert;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertRescheduled;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertScheduled;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class ConcertSummaryProjector {

    private final EventStore<ConcertId, ConcertEvent, Concert> concertStore;

    public ConcertSummaryProjector(EventStore<ConcertId, ConcertEvent, Concert> concertStore) {
        this.concertStore = concertStore;
    }

    public Stream<ConcertSummary> allConcertSummaries() {
        Map<ConcertId, ConcertSummary> views = new HashMap<>();
        concertStore.allEvents()
                    .forEach(concertEvent -> {
                                 switch (concertEvent) {
                                     case ConcertScheduled(
                                             ConcertId concertId, _,
                                             String artist,
                                             int ticketPrice,
                                             LocalDateTime showDateTime,
                                             LocalTime doorsTime,
                                             _, _) ->
                                             views.put(concertId,
                                                    new ConcertSummary(concertId, artist, ticketPrice, showDateTime, doorsTime));

                                     case ConcertRescheduled(ConcertId concertId, _,
                                                             LocalDateTime newShowDateTime, LocalTime newDoorsTime) -> {
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

    public ConcertWithEvents concertWithEventsFor(ConcertId concertId) {
        List<ConcertEvent> concertEvents = concertStore
                .eventsForAggregate(concertId);
        Concert concert = Concert.reconstitute(concertEvents);
        return new ConcertWithEvents(concertEvents, concert);
    }

    public ConcertWithEvents concertWithEventsThrough(ConcertId concertId, Long desiredEventSequenceNumber) {
        List<ConcertEvent> concertEvents = concertStore
                .eventsForAggregate(concertId)
                .stream()
                .filter(concertEvent -> concertEvent.eventSequence() <= desiredEventSequenceNumber)
                .toList();
        Concert concert = Concert.reconstitute(concertEvents);
        return new ConcertWithEvents(concertEvents, concert);
    }

    public record ConcertWithEvents(List<ConcertEvent> concertEvents, Concert concert) {}
}
