package dev.ted.jitterticket.eventviewer.adapter.in.web;

import dev.ted.jitterticket.eventsourced.application.AvailableConcertsProjector;
import dev.ted.jitterticket.eventsourced.application.port.EventStore;
import dev.ted.jitterticket.eventsourced.domain.Event;
import dev.ted.jitterticket.eventsourced.domain.concert.Concert;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;

import java.util.List;
import java.util.UUID;

public class ConcertProjectionChoice extends ProjectionChoice {

    private final EventStore<ConcertId, ConcertEvent, Concert> concertStore;
    protected AvailableConcertsProjector availableConcertsProjector;

    public ConcertProjectionChoice(EventStore<ConcertId, ConcertEvent, Concert> concertStore) {
        super("Concert", "concerts");
        this.concertStore = concertStore;
        this.availableConcertsProjector = new AvailableConcertsProjector(concertStore);
    }

    @Override
    public List<AggregateSummaryView> aggregateSummaryViews() {
        return availableConcertsProjector
                .availableConcerts()
                .map(concertSummary -> new AggregateSummaryView(
                        concertSummary.concertId().id().toString(),
                        concertSummary.artist()
                ))
                .toList();
    }

    @Override
    public List<? extends Event> eventsFor(UUID uuid) {
        return concertStore.eventsForAggregate(new ConcertId(uuid));
    }

    @Override
    public List<String> propertiesOfProjectionFrom(List<? extends Event> events) {
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
