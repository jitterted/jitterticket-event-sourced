package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.adapter.out.store.jdbc.ConcertSalesDbo;
import dev.ted.jitterticket.eventsourced.adapter.out.store.jdbc.ConcertSalesProjectionDbo;
import dev.ted.jitterticket.eventsourced.adapter.out.store.jdbc.ConcertSalesProjectionRepository;
import dev.ted.jitterticket.eventsourced.application.port.EventStore;
import dev.ted.jitterticket.eventsourced.domain.concert.Concert;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ConcertSalesProjectionMediator implements EventConsumer<ConcertEvent> {

    private static final Logger log = LoggerFactory.getLogger(ConcertSalesProjectionMediator.class);

    static final String PROJECTION_NAME = "concert_sales_projector";
    private final ConcertSalesProjector concertSalesProjector;
    private final EventStore<ConcertId, ConcertEvent, Concert> concertEventStore;
    private final ConcertSalesProjectionRepository concertSalesProjectionRepository;

    public ConcertSalesProjectionMediator(ConcertSalesProjector concertSalesProjector,
                                          EventStore<ConcertId, ConcertEvent, Concert> concertEventStore,
                                          ConcertSalesProjectionRepository concertSalesProjectionRepository) {
        this.concertSalesProjector = concertSalesProjector;
        this.concertEventStore = concertEventStore;
        this.concertSalesProjectionRepository = concertSalesProjectionRepository;

        log.debug("Fetching last event sequence seen by the projection");
        long lastEventSequenceSeen = concertSalesProjectionRepository
                .findLastEventSequenceSeenByProjectionName(PROJECTION_NAME)
                .orElse(0L);

        log.debug("Fetching all events after the last event sequence: {}...", lastEventSequenceSeen);
        Stream<ConcertEvent> concertEventStream =
                this.concertEventStore.allEventsAfter(lastEventSequenceSeen);
        log.debug("Fetched all events after last event sequence: {}", lastEventSequenceSeen);

        log.debug("Starting event handling for projection...");
        handle(concertEventStream);
        log.debug("Completed event handling for projection");

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

    @Override
    public void handle(Stream<ConcertEvent> eventStream) {
        ConcertSalesProjectionDbo concertSalesProjectionDbo =
                concertSalesProjectionRepository
                        .findById(PROJECTION_NAME)
                        .orElse(createNewProjectionDbo());

        log.debug("Converting {} Sales DBOs to Sales Summaries...", concertSalesProjectionDbo.getConcertSales().size());
        Map<ConcertId, ConcertSalesProjector.ConcertSalesSummary> salesSummaryMap =
                concertSalesProjectionDbo.getConcertSales()
                                         .stream()
                                         .map(ConcertSalesProjectionMediator::dboToSummary)
                                         .collect(Collectors.toMap(ConcertSalesProjector.ConcertSalesSummary::concertId,
                                                             Function.identity()));

        log.debug("Starting projection calculation...");
        ConcertSalesProjector.ProjectionResult result =
                concertSalesProjector.project(salesSummaryMap,
                                              eventStream,
                                              concertSalesProjectionDbo.getLastEventSequenceSeen());
        log.debug("Projection calculation completed, mapping Sales Summary to DBOs...");

        Set<ConcertSalesDbo> concertSalesDbos =
                result.salesSummaries()
                      .stream()
                      .map(ConcertSalesProjectionMediator::summaryToDbo)
                      .collect(Collectors.toUnmodifiableSet());

        concertSalesProjectionDbo.setLastEventSequenceSeen(
                result.lastEventSequenceSeen());
        concertSalesProjectionDbo.setConcertSales(concertSalesDbos);

        log.debug("Saving concert sales projection with {} summaries to Repository", concertSalesDbos.size());
        concertSalesProjectionRepository.save(concertSalesProjectionDbo);
    }

    private static ConcertSalesProjectionDbo createNewProjectionDbo() {
        return new ConcertSalesProjectionDbo(
                PROJECTION_NAME, 0L);
    }

    public Stream<ConcertSalesProjector.ConcertSalesSummary> allSalesSummaries() {
        return concertSalesProjectionRepository
                .findById(PROJECTION_NAME)
                .orElse(createNewProjectionDbo())
                .getConcertSales()
                .stream()
                .map(ConcertSalesProjectionMediator::dboToSummary);
    }

}
