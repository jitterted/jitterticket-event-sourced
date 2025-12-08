package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertRescheduled;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertScheduled;
import dev.ted.jitterticket.eventsourced.domain.concert.TicketsSold;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

public class MakeEvents {
    private int eventSequence = 0;
    private final List<ConcertEvent> events = new ArrayList<>();

    public static MakeEvents with() {
        return new MakeEvents();
    }


    public Stream<ConcertEvent> stream() {
        return events.stream();
    }

    public MakeEvents concertScheduled(ConcertId concertId) {
        ConcertScheduled concertScheduled =
                createConcertScheduled(concertId, 42, "Don't Care Artist Name");
        events.add(concertScheduled);
        return this;
    }

    public MakeEvents concertScheduled(ConcertId concertId, Function<ConcertCustomizer, ConcertCustomizer> concertCustomizer) {
        ConcertCustomizer customizer = concertCustomizer.apply(new ConcertCustomizer());
        ConcertScheduled concertScheduled =
                createConcertScheduled(concertId,
                                       customizer.ticketPrice,
                                       customizer.artistName);
        events.add(concertScheduled);
        customizer.ticketsSoldQuantity
                .stream()
                .map(qty -> new TicketsSold(
                        concertId,
                        eventSequence++,
                        qty,
                        qty * customizer.ticketPrice))
                .forEach(events::add);
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
                                       eventSequence++,
                                       newShowDateTime,
                                       newDoorsTime));
        return this;
    }

    private ConcertScheduled createConcertScheduled(ConcertId concertId, int ticketPrice, String artistName) {
        return new ConcertScheduled(concertId,
                                    eventSequence++,
                                    artistName,
                                    ticketPrice,
                                    LocalDateTime.now(),
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
    }
}
