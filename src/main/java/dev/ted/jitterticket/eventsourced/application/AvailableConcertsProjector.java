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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class AvailableConcertsProjector implements
        DomainProjector<ConcertEvent, AvailableConcerts, AvailableConcertsDelta> {

    @Override
    public ProjectorResult<AvailableConcerts, AvailableConcertsDelta>
    project(AvailableConcerts currentState,
            Stream<ConcertEvent> concertEventStream) {
        Map<ConcertId, AvailableConcert> availableConcertsMap =
                loadMapFrom(currentState);
        List<ConcertId> removedConcertIds = new ArrayList<>();
        Map<ConcertId, AvailableConcert> insertedConcerts = new HashMap<>();
        Map<ConcertId, AvailableConcert> updatedConcerts = new HashMap<>();

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
                case ConcertRescheduled rescheduled -> {
                    ConcertId concertId = rescheduled.concertId();
                    AvailableConcert oldConcert = availableConcertsMap.get(concertId);
                    AvailableConcert rescheduledView =
                            rescheduleTo(rescheduled.newShowDateTime(),
                                         rescheduled.newDoorsTime(),
                                         oldConcert);
                    availableConcertsMap.put(concertId, rescheduledView);
                    updatedConcerts.put(concertId, rescheduledView);
                }
                case TicketsSold _ -> {
                    // don't care about this event for this projector
                }
                case TicketSalesStopped ticketSalesStopped -> {
                    ConcertId concertId = ticketSalesStopped.concertId();
                    availableConcertsMap.remove(concertId);
                    if (shouldMarkAsRemoved(insertedConcerts, concertId)) {
                        removedConcertIds.add(concertId);
                    }
                    insertedConcerts.remove(concertId);
                    updatedConcerts.remove(concertId);
                }
            }
        });

        Map<ConcertId, AvailableConcert> upsertedConcertsMap = new HashMap<>(insertedConcerts);
        upsertedConcertsMap.putAll(updatedConcerts);
        return new ProjectorResult<>(
                new AvailableConcerts(List.copyOf(availableConcertsMap.values())),
                new AvailableConcertsDelta(List.copyOf(upsertedConcertsMap.values()),
                                           removedConcertIds)
        );
    }

    private boolean shouldMarkAsRemoved(Map<ConcertId, AvailableConcert> insertedConcerts, ConcertId concertId) {
        return !insertedConcerts.containsKey(concertId);
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
