package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.application.port.EventStore;
import dev.ted.jitterticket.eventsourced.domain.concert.Concert;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertRescheduled;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertScheduled;
import dev.ted.jitterticket.eventsourced.domain.concert.TicketsSold;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

@SuppressWarnings("ClassCanBeRecord")
public class ConcertSalesProjector {

    private final EventStore<ConcertId, ConcertEvent, Concert> concertEventStore;

    private ConcertSalesProjector(EventStore<ConcertId, ConcertEvent, Concert> concertEventStore) {
        this.concertEventStore = concertEventStore;
    }

    public static ConcertSalesProjector createForTest(EventStore<ConcertId, ConcertEvent, Concert> concertEventStore) {
        return new ConcertSalesProjector(concertEventStore);
    }

    public static ConcertSalesProjector createForTest() {
        return new ConcertSalesProjector(InMemoryEventStore.forConcerts());
    }


    // ? replace with something like:
    // new SalesSummarizer().apply(events) -> Stream<ConcertSalesSummary>
    // ?
    public Stream<ConcertSalesSummary> allSalesSummaries() {
        Map<ConcertId, ConcertSalesSummary> summaries = new HashMap<>();
        concertEventStore
                .allEvents()
                .forEach(concertEvent -> {
                    switch (concertEvent) {
                        case ConcertScheduled concertScheduled -> summaries.put(
                                concertScheduled.concertId(),
                                createSummaryFrom(concertScheduled));
                        case TicketsSold ticketsSold -> summaries.computeIfPresent(
                                ticketsSold.concertId(),
                                (_, summary) -> summary.plusTicketsSold(ticketsSold));
                        case ConcertRescheduled concertRescheduled -> summaries.computeIfPresent(
                                concertRescheduled.concertId(),
                                (_, summary) -> summary.withNewShowDateTime(concertRescheduled.newShowDateTime()));
                    }
                });
        return summaries.values().stream();
    }

    private static ConcertSalesSummary createSummaryFrom(ConcertScheduled concertScheduled) {
        return new ConcertSalesSummary(
                concertScheduled.concertId(),
                concertScheduled.artist(),
                concertScheduled.showDateTime(),
                0, // tickets sold
                0  // ticket $$ sales
        );
    }


    public record ConcertSalesSummary(ConcertId concertId, String artist,
                                      LocalDateTime showDateTime,
                                      int totalQuantity, int totalSales) {
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
