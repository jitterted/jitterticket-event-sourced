package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.adapter.out.store.jdbc.ConcertSalesProjectionDbo;
import dev.ted.jitterticket.eventsourced.adapter.out.store.jdbc.ConcertSalesProjectionRepository;
import dev.ted.jitterticket.eventsourced.application.port.EventStore;
import dev.ted.jitterticket.eventsourced.domain.concert.Concert;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;

import java.util.stream.Stream;

public class ConcertSalesProjectionMediator {

    private final ConcertSalesProjector concertSalesProjector;
    private final EventStore<ConcertId, ConcertEvent, Concert> concertEventStore;
    private final ConcertSalesProjectionRepository concertSalesProjectionRepository;

    public ConcertSalesProjectionMediator(ConcertSalesProjector concertSalesProjector,
                                          EventStore<ConcertId, ConcertEvent, Concert> concertEventStore,
                                          ConcertSalesProjectionRepository concertSalesProjectionRepository) {
        this.concertSalesProjector = concertSalesProjector;
        this.concertEventStore = concertEventStore;
        this.concertSalesProjectionRepository = concertSalesProjectionRepository;

        // TODO: create convenience query for getting the event sequence checkpoint
        ConcertSalesProjectionDbo loadedConcertSalesProjectionDbo =
                this.concertSalesProjectionRepository
                        .findById(ConcertSalesProjector.PROJECTION_NAME)
                        .orElse(createNewProjectionDbo());
        Stream<ConcertEvent> concertEventStream =
                this.concertEventStore.allEventsAfter(
                        loadedConcertSalesProjectionDbo.getLastEventSequenceSeen());

        handle(concertEventStream);

        concertEventStore.subscribe(this);
    }

    public void handle(Stream<ConcertEvent> concertEventStream) {
        ConcertSalesProjectionDbo loadedProjectionDbo =
                concertSalesProjectionRepository
                        .findById(ConcertSalesProjector.PROJECTION_NAME)
                        .orElse(createNewProjectionDbo());

        ConcertSalesProjectionDbo concertSalesProjectionDbo
                = concertSalesProjector.project(loadedProjectionDbo,
                                                concertEventStream);

        concertSalesProjectionRepository.save(concertSalesProjectionDbo);
    }

    private static ConcertSalesProjectionDbo createNewProjectionDbo() {
        return new ConcertSalesProjectionDbo(
                ConcertSalesProjector.PROJECTION_NAME, 0L);
    }

    public Stream<ConcertSalesProjector.ConcertSalesSummary> allSalesSummaries() {
        return concertSalesProjectionRepository
                .findById(ConcertSalesProjector.PROJECTION_NAME)
                .orElse(createNewProjectionDbo())
                .getConcertSales()
                .stream()
                .map(ConcertSalesProjector::dboToSummary);
    }

}
