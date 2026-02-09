package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertRescheduled;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertScheduled;
import dev.ted.jitterticket.eventsourced.domain.concert.TicketSalesStopped;
import dev.ted.jitterticket.eventsourced.domain.concert.TicketsSold;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

public class MakeEvents {
    private final List<ConcertEvent> events = new ArrayList<>();
    // need to simulate a monotonically increasing sequence
    private final Iterator<Long> eventSequenceIterator;

    public MakeEvents() {
        this.eventSequenceIterator = new Iterator<>() {
            private long eventSequence = 1;

            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public Long next() {
                return eventSequence++;
            }
        };
    }

    public MakeEvents(Iterator<Long> eventSequenceIterator) {
        this.eventSequenceIterator = eventSequenceIterator;
    }

    public static MakeEvents with() {
        return new MakeEvents();
    }

    public static MakeEvents withNullEventSequences() {
        Iterator<Long> nullSupplyingIterator = new Iterator<>() {
            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public Long next() {
                return null;
            }
        };
        return new MakeEvents(nullSupplyingIterator);
    }


    public Stream<ConcertEvent> stream() {
        return events.stream();
    }

    public MakeEvents concertScheduled(ConcertId concertId) {
        ConcertScheduled concertScheduled =
                createConcertScheduled(concertId, 42, "Don't Care Artist Name", LocalDateTime.now());
        events.add(concertScheduled);
        return this;
    }

    public MakeEvents concertScheduled(ConcertId concertId, Function<ConcertCustomizer, ConcertCustomizer> concertCustomizer) {
        ConcertCustomizer customizer = concertCustomizer.apply(new ConcertCustomizer());
        ConcertScheduled concertScheduled =
                createConcertScheduled(concertId,
                                       customizer.ticketPrice,
                                       customizer.artistName,
                                       customizer.showDateTime);
        events.add(concertScheduled);
        customizer.ticketsSoldQuantity
                .stream()
                .map(qty -> new TicketsSold(
                        concertId,
                        eventSequenceIterator.next(),
                        qty,
                        qty * customizer.ticketPrice))
                .forEach(events::add);
        if (customizer.isTicketSalesStopped()) {
            events.add(new TicketSalesStopped(
                    concertId, eventSequenceIterator.next()));
        }
        return this;
    }

    /**
     * Don't care about any of the values (ID, artist, etc.) in the event
     */
    public MakeEvents concertScheduled() {
        return concertScheduled(ConcertId.createRandom());
    }

    public MakeEvents reschedule(ConcertId concertId, LocalDateTime newShowDateTime, LocalTime newDoorsTime) {
        events.add(
                new ConcertRescheduled(concertId,
                                       eventSequenceIterator.next(),
                                       newShowDateTime,
                                       newDoorsTime));
        return this;
    }

    private ConcertScheduled createConcertScheduled(ConcertId concertId, int ticketPrice, String artistName, LocalDateTime showDateTime) {
        return new ConcertScheduled(concertId,
                                    eventSequenceIterator.next(),
                                    artistName,
                                    ticketPrice,
                                    showDateTime,
                                    LocalTime.now(),
                                    100,
                                    8);
    }

    public List<ConcertEvent> list() {
        return List.copyOf(events);
    }

    public static class ConcertCustomizer {

        private int ticketPrice = 42; // default
        private final List<Integer> ticketsSoldQuantity = new ArrayList<>();
        private String artistName = "Don't Care Artist Name"; // default
        private LocalDateTime showDateTime = LocalDateTime.now();
        private boolean ticketSalesStopped = false;

        public ConcertCustomizer ticketPrice(int ticketPrice) {
            this.ticketPrice = ticketPrice;
            return this;
        }

        public ConcertCustomizer ticketsSold(int quantity) {
            ticketsSoldQuantity.add(quantity);
            return this;
        }

        public ConcertCustomizer artistNamed(String artistName) {
            this.artistName = artistName;
            return this;
        }

        public ConcertCustomizer showDateTime(LocalDateTime showDateTime) {
            this.showDateTime = showDateTime;
            return this;
        }

        public ConcertCustomizer ticketSalesStopped() {
            ticketSalesStopped = true;
            return this;
        }

        public boolean isTicketSalesStopped() {
            return ticketSalesStopped;
        }
    }
}
