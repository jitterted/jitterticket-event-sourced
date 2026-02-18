package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertRescheduled;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertScheduled;
import dev.ted.jitterticket.eventsourced.domain.concert.TicketSalesStopped;
import dev.ted.jitterticket.eventsourced.domain.concert.TicketsSold;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class AllConcertsProjector implements
        DomainProjector<ConcertEvent, AvailableConcerts, AvailableConcertsDelta> {

    @Override
    public ProjectorResult<AvailableConcerts, AvailableConcertsDelta>
    project(AvailableConcerts currentState, Stream<ConcertEvent> concertEventStream) {
        Map<ConcertId, AvailableConcert> availableConcertsMap = loadMapFrom(currentState);
        Map<ConcertId, AvailableConcert> insertedConcerts = new HashMap<>();

        concertEventStream.forEach(concertEvent -> {
            switch (concertEvent) {
                case ConcertScheduled scheduled -> {
                    ConcertId concertId = scheduled.concertId();
                    AvailableConcert availableConcert =
                            new AvailableConcert(concertId,
                                                 scheduled.artist(),
                                                 scheduled.ticketPrice(),
                                                 scheduled.showDateTime(),
                                                 scheduled.doorsTime());
                    availableConcertsMap.put(concertId, availableConcert);
                    insertedConcerts.put(concertId, availableConcert);
                }

                case ConcertRescheduled concertRescheduled -> {
                }
                case TicketSalesStopped ticketSalesStopped -> {
                }
                case TicketsSold ticketsSold -> {
                }
            }
        });

        Map<ConcertId, AvailableConcert> upsertedConcertsMap = new HashMap<>(insertedConcerts);
        return new ProjectorResult<>(
                new AvailableConcerts(List.copyOf(availableConcertsMap.values())),
                new AvailableConcertsDelta(List.copyOf(upsertedConcertsMap.values()),
                                           Collections.emptyList())
        );
    }

    private Map<ConcertId, AvailableConcert> loadMapFrom(AvailableConcerts currentState) {
        Map<ConcertId, AvailableConcert> availableConcertsMap = new HashMap<>();
        currentState.availableConcerts().forEach(concert -> availableConcertsMap.put(concert.concertId(), concert));
        return availableConcertsMap;
    }

}
