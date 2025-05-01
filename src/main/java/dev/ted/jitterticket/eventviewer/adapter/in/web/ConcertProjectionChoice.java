package dev.ted.jitterticket.eventviewer.adapter.in.web;

import dev.ted.jitterticket.eventsourced.application.ConcertSummaryProjector;
import dev.ted.jitterticket.eventsourced.application.EventStore;
import dev.ted.jitterticket.eventsourced.domain.Event;
import dev.ted.jitterticket.eventsourced.domain.concert.Concert;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;

import java.util.List;
import java.util.UUID;

public class ConcertProjectionChoice extends ProjectionChoice {

    private final EventStore<ConcertId, ConcertEvent, Concert> concertStore;

    public ConcertProjectionChoice(EventStore<ConcertId, ConcertEvent, Concert> concertStore) {
        super("Concert", "/event-viewer/concerts", "Concerts");
        this.concertStore = concertStore;
    }

    @Override
    public List<AggregateSummaryView> aggregateSummaryViews() {
        return new ConcertSummaryProjector(concertStore).allConcertSummaries().map(AggregateSummaryView::of).toList();
    }

    @Override
    public List<? extends Event> eventsFor(UUID uuid) {
        return concertStore.eventsForAggregate(new ConcertId(uuid));
    }

    @Override
    public List<String> propertiesOfAggregateFrom(List<? extends Event> events) {
        @SuppressWarnings("unchecked")
        Concert concert = Concert.reconstitute((List<ConcertEvent>) events);
        return List.of(
                "Artist: " + concert.artist(),
                "Show Time: " + concert.showDateTime(),
                "Doors Time: " + concert.doorsTime(),
                "Tickets Remaining: " + concert.availableTicketCount()
        );
    }

}
