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

public class ScheduledConcertsProjector implements
        DomainProjector<ConcertEvent, ScheduledConcerts, ScheduledConcertsDelta> {

    private static final List<ConcertId> ALWAYS_EMPTY_REMOVED_CONCERT_IDS = Collections.emptyList();

    @Override
    public ProjectorResult<ScheduledConcerts, ScheduledConcertsDelta>
    project(ScheduledConcerts currentState,
            Stream<ConcertEvent> concertEventStream) {

        Map<ConcertId, ScheduledConcert> scheduledConcertsMap = new HashMap<>();
//                loadMapFrom(currentState);
        Map<ConcertId, ScheduledConcert> insertedConcerts = new HashMap<>();
        Map<ConcertId, ScheduledConcert> updatedConcerts = new HashMap<>();

        concertEventStream.forEach(
                concertEvent -> {
                    switch (concertEvent) {
                        case ConcertScheduled scheduled -> {
                            ConcertId concertId = scheduled.concertId();
                            ScheduledConcert scheduledConcert = new ScheduledConcert(
                                    concertId, scheduled.showDateTime().toLocalDate());
                            scheduledConcertsMap.put(concertId, scheduledConcert);
                            insertedConcerts.put(concertId, scheduledConcert);
                        }
                        case ConcertRescheduled concertRescheduled -> {
                        }
                        case TicketSalesStopped _, TicketsSold _ -> {
                        }
                    }
                }
        );

        Map<ConcertId, ScheduledConcert> upsertedConcertsMap = new HashMap<>(insertedConcerts);
        upsertedConcertsMap.putAll(updatedConcerts);
        return new ProjectorResult<>(
                new ScheduledConcerts(
                        List.copyOf(scheduledConcertsMap.values())),
                new ScheduledConcertsDelta(
                        List.copyOf(upsertedConcertsMap.values()),
                        ALWAYS_EMPTY_REMOVED_CONCERT_IDS
                ));
    }

//    private Map<ConcertId, AvailableConcert> loadMapFrom(AvailableConcerts currentState) {
//        Map<ConcertId, AvailableConcert> availableConcertsMap = new HashMap<>();
//        currentState.availableConcerts().forEach(concert -> availableConcertsMap.put(concert.concertId(), concert));
//        return availableConcertsMap;
//    }

}
