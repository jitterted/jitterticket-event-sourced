package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.application.port.EventStore;
import dev.ted.jitterticket.eventsourced.domain.concert.Concert;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertRescheduled;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertScheduled;
import dev.ted.jitterticket.eventsourced.domain.concert.TicketsSold;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class ConcertSummaryProjector implements EventConsumer<ConcertEvent> {

    protected final Map<ConcertId, ConcertSummary> concertSummaryMap = new HashMap<>();

    public ConcertSummaryProjector(EventStore<ConcertId, ConcertEvent, Concert> concertStore) {
        concertStore.allEventsAfter(0L).forEach(this::apply);
        concertStore.subscribe(this);
    }

    public Stream<ConcertSummary> allConcertSummaries() {
        return concertSummaryMap.values().stream();
    }

    @Override
    public void handle(Stream<ConcertEvent> concertEventStream) {
        concertEventStream.forEach(this::apply);
    }

    private void apply(ConcertEvent concertEvent) {
        switch (concertEvent) {
            case ConcertScheduled scheduled -> concertSummaryMap.put(scheduled.concertId(),
                                                                     new ConcertSummary(scheduled.concertId(),
                                                                                        scheduled.artist(),
                                                                                        scheduled.ticketPrice(),
                                                                                        scheduled.showDateTime(),
                                                                                        scheduled.doorsTime()));
            case ConcertRescheduled rescheduled -> {
                ConcertSummary oldView = concertSummaryMap.get(rescheduled.concertId());
                ConcertSummary rescheduledView = rescheduleTo(rescheduled.newShowDateTime(),
                                                              rescheduled.newDoorsTime(),
                                                              oldView);
                concertSummaryMap.put(rescheduled.concertId(), rescheduledView);
            }
            case TicketsSold ticketsSold -> {
                // don't care about this event for this projector
            }
        }
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
