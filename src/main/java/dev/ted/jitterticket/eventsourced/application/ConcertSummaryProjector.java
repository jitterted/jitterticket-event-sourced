package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.application.port.EventStore;
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

public class ConcertSummaryProjector {

    private final EventStore<ConcertId, ConcertEvent, Concert> concertStore;

    public ConcertSummaryProjector(EventStore<ConcertId, ConcertEvent, Concert> concertStore) {
        this.concertStore = concertStore;
    }

    public Stream<ConcertSummary> allConcertSummaries() {
        Map<ConcertId, ConcertSummary> views = new HashMap<>();
        concertStore.allEvents()
                    .forEach(concertEvent -> {
                                 if (concertEvent instanceof ConcertScheduled scheduled) {
                                     views.put(scheduled.concertId(),
                                             new ConcertSummary(scheduled.concertId(), 
                                                               scheduled.artist(), 
                                                               scheduled.ticketPrice(), 
                                                               scheduled.showDateTime(), 
                                                               scheduled.doorsTime()));
                                 } else if (concertEvent instanceof ConcertRescheduled rescheduled) {
                                     ConcertSummary oldView = views.get(rescheduled.concertId());
                                     ConcertSummary rescheduledView = rescheduleTo(rescheduled.newShowDateTime(), 
                                                                                  rescheduled.newDoorsTime(), 
                                                                                  oldView);
                                     views.put(rescheduled.concertId(), rescheduledView);
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
