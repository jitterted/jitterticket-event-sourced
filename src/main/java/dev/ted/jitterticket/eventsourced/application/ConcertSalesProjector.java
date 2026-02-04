package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertRescheduled;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertScheduled;
import dev.ted.jitterticket.eventsourced.domain.concert.TicketSalesStopped;
import dev.ted.jitterticket.eventsourced.domain.concert.TicketsSold;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

public class ConcertSalesProjector {
    private static final Logger log = LoggerFactory.getLogger(ConcertSalesProjector.class);

    ProjectionResult project(Map<ConcertId, ConcertSalesSummary> salesSummaryMap,
                             Stream<ConcertEvent> concertEvents,
                             long lastEventSequenceSeen) {
        Map<ConcertId, ConcertSalesSummary> mutableMap = new HashMap<>(salesSummaryMap);

        AtomicReference<Long> lastEventSequenceSeenRef = new AtomicReference<>(lastEventSequenceSeen);
        AtomicInteger eventSequenceCounter = new AtomicInteger(0);

        concertEvents
                .forEach(concertEvent -> {
                    if (concertEvent.eventSequence() == null) {
                        throw new IllegalStateException("Event sequence cannot be null for event: " + concertEvent);
                    }
                    switch (concertEvent) {
                        case ConcertScheduled concertScheduled -> mutableMap.put(
                                concertScheduled.concertId(),
                                ConcertSalesSummary.createSummaryFrom(concertScheduled));
                        case TicketsSold ticketsSold -> mutableMap.computeIfPresent(
                                ticketsSold.concertId(),
                                (_, summary) -> summary.plusTicketsSold(ticketsSold));
                        case ConcertRescheduled concertRescheduled -> mutableMap.computeIfPresent(
                                concertRescheduled.concertId(),
                                (_, summary) -> summary.withNewShowDateTime(concertRescheduled.newShowDateTime()));
                        case TicketSalesStopped ticketSalesStopped -> {
                            // ignored: we don't care (unless we want to show this on the sales report screen)
                        }
                    }
                    lastEventSequenceSeenRef.set(concertEvent.eventSequence());
                    eventSequenceCounter.incrementAndGet();
                });

        log.debug("Projection calculation completed, processed {} events", eventSequenceCounter.get());
        return new ProjectionResult(mutableMap.values(),
                                    lastEventSequenceSeenRef.get());
    }

    record ProjectionResult(Collection<ConcertSalesSummary> salesSummaries,
                            long lastEventSequenceSeen) {}


    public record ConcertSalesSummary(
            ConcertId concertId,
            String artist,
            LocalDateTime showDateTime,
            int totalQuantity,
            int totalSales
    ) {
        private static ConcertSalesSummary createSummaryFrom(ConcertScheduled concertScheduled) {
            return new ConcertSalesSummary(
                    concertScheduled.concertId(),
                    concertScheduled.artist(),
                    concertScheduled.showDateTime(),
                    0, // tickets sold
                    0  // ticket $$ sales
            );
        }

        public ConcertSalesSummary plusTicketsSold(TicketsSold ticketsSold) {
            return new ConcertSalesSummary(
                    concertId, artist,
                    showDateTime,
                    totalQuantity + ticketsSold.quantity(),
                    totalSales + ticketsSold.totalPaid());
        }

        public ConcertSalesSummary withNewShowDateTime(LocalDateTime newShowDateTime) {
            return new ConcertSalesSummary(
                    concertId, artist,
                    newShowDateTime,
                    totalQuantity, totalSales);
        }
    }
}
