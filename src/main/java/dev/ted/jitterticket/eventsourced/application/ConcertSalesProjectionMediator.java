package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.adapter.out.store.jdbc.ConcertSalesDbo;
import dev.ted.jitterticket.eventsourced.adapter.out.store.jdbc.ConcertSalesProjectionDbo;
import dev.ted.jitterticket.eventsourced.adapter.out.store.jdbc.ConcertSalesProjectionRepository;
import dev.ted.jitterticket.eventsourced.application.port.EventStore;
import dev.ted.jitterticket.eventsourced.domain.concert.Concert;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
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

    static ConcertSalesProjector.ConcertSalesSummary dboToSummary(ConcertSalesDbo concertSalesDbo) {
        return new ConcertSalesProjector.ConcertSalesSummary(
                new ConcertId(concertSalesDbo.getConcertId()),
                concertSalesDbo.getArtistName(),
                concertSalesDbo.getConcertDate().atStartOfDay(),
                concertSalesDbo.getTicketsSold(),
                concertSalesDbo.getTotalSales()
        );
    }

    static ConcertSalesDbo summaryToDbo(ConcertSalesProjector.ConcertSalesSummary summary) {
        return new ConcertSalesDbo(
                summary.concertId().id(),
                summary.artist(),
                summary.showDateTime().toLocalDate(),
                summary.totalQuantity(),
                summary.totalSales()
        );
    }

    public void handle(Stream<ConcertEvent> concertEventStream) {
        ConcertSalesProjectionDbo concertSalesProjectionDbo =
                concertSalesProjectionRepository
                        .findById(ConcertSalesProjector.PROJECTION_NAME)
                        .orElse(createNewProjectionDbo());

        Map<ConcertId, ConcertSalesProjector.ConcertSalesSummary> salesSummaryMap =
                concertSalesProjectionDbo.getConcertSales()
                                         .stream()
                                         .map(ConcertSalesProjectionMediator::dboToSummary)
                                         .collect(Collectors.toMap(ConcertSalesProjector.ConcertSalesSummary::concertId,
                                                             Function.identity()));

        ConcertSalesProjector.ProjectionResult result = concertSalesProjector.project(salesSummaryMap, concertEventStream);

        Set<ConcertSalesDbo> concertSalesDbos =
                result.salesSummaries()
                      .stream()
                      .map(ConcertSalesProjectionMediator::summaryToDbo)
                      .collect(Collectors.toUnmodifiableSet());

        concertSalesProjectionDbo.setLastEventSequenceSeen(
                result.lastEventSequenceSeen());
        concertSalesProjectionDbo.setConcertSales(concertSalesDbos);

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
                .map(ConcertSalesProjectionMediator::dboToSummary);
    }

}
