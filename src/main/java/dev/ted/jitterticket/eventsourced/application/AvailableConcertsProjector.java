package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertRescheduled;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertScheduled;
import dev.ted.jitterticket.eventsourced.domain.concert.TicketSalesStopped;
import dev.ted.jitterticket.eventsourced.domain.concert.TicketsSold;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class AvailableConcertsProjector implements
        DomainProjector<ConcertEvent, AvailableConcerts, AvailableConcertsDelta> {

    private static final List<ConcertId> ALWAYS_EMPTY_CONCERT_IDS_TO_BE_REMOVED = List.of();

    @Override
    public ProjectorResult<AvailableConcerts, AvailableConcertsDelta>
        project(AvailableConcerts currentState,
                Stream<ConcertEvent> concertEventStream) {
        Map<ConcertId, AvailableConcert> availableConcertsMap =
                loadMapFrom(currentState);

        Map<ConcertId, AvailableConcert> deltaMap = new HashMap<>();

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
                    deltaMap.put(concertId, availableConcert);
                }
                case ConcertRescheduled rescheduled -> {
                    ConcertId concertId = rescheduled.concertId();
                    AvailableConcert oldConcert = availableConcertsMap.get(concertId);
                    AvailableConcert rescheduledView =
                            rescheduleTo(rescheduled.newShowDateTime(),
                                         rescheduled.newDoorsTime(),
                                         oldConcert);
                    availableConcertsMap.put(concertId, rescheduledView);
                    deltaMap.put(concertId, rescheduledView);
                }
                case TicketsSold ticketsSold -> {
                    // don't care about this event for this projector
                }
                case TicketSalesStopped ticketSalesStopped -> {
                    // TBD: remove this concert
                }
            }
        });

        return new ProjectorResult<>(
                new AvailableConcerts(List.copyOf(availableConcertsMap.values())),
                new AvailableConcertsDelta(List.copyOf(deltaMap.values()),
                                           ALWAYS_EMPTY_CONCERT_IDS_TO_BE_REMOVED)
        );
    }

    private Map<ConcertId, AvailableConcert> loadMapFrom(AvailableConcerts currentState) {
        Map<ConcertId, AvailableConcert> availableConcertsMap = new HashMap<>();
        currentState.availableConcerts().forEach(concert -> availableConcertsMap.put(concert.concertId(), concert));
        return availableConcertsMap;
    }

    private AvailableConcert rescheduleTo(LocalDateTime newShowDateTime,
                                          LocalTime newDoorsTime,
                                          AvailableConcert oldConcert) {
        return new AvailableConcert(
                oldConcert.concertId(),
                oldConcert.artist(),
                oldConcert.ticketPrice(),
                newShowDateTime,
                newDoorsTime
        );
    }
}
