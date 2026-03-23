package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertRescheduled;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertScheduled;
import dev.ted.jitterticket.eventsourced.domain.concert.TicketSalesStopped;
import dev.ted.jitterticket.eventsourced.domain.concert.TicketsSold;

import java.time.LocalDateTime;
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

        Map<ConcertId, ScheduledConcert> scheduledConcertsMap =
                loadMapFrom(currentState);
        Map<ConcertId, ScheduledConcert> upsertedConcertsMap = new HashMap<>();

        concertEventStream.forEach(
                concertEvent -> {
                    switch (concertEvent) {
                        case ConcertScheduled scheduled -> {
                            addToMaps(scheduled.concertId(),
                                      scheduled.showDateTime(),
                                      scheduledConcertsMap,
                                      upsertedConcertsMap);
                        }
                        case ConcertRescheduled rescheduled -> {
                            addToMaps(rescheduled.concertId(),
                                      rescheduled.newShowDateTime(),
                                      scheduledConcertsMap,
                                      upsertedConcertsMap);
                        }
                        case TicketSalesStopped _, TicketsSold _ -> {
                        }
                    }
                }
        );

        return new ProjectorResult<>(
                new ScheduledConcerts(List.copyOf(scheduledConcertsMap.values())),
                new ScheduledConcertsDelta(
                        List.copyOf(upsertedConcertsMap.values()),
                        ALWAYS_EMPTY_REMOVED_CONCERT_IDS
                ));
    }

    private void addToMaps(ConcertId concertId, LocalDateTime showDateTime, Map<ConcertId, ScheduledConcert> scheduledConcertsMap, Map<ConcertId, ScheduledConcert> upsertedConcertsMap) {
        ScheduledConcert scheduledConcert = new ScheduledConcert(
                concertId, showDateTime.toLocalDate());
        scheduledConcertsMap.put(concertId, scheduledConcert);
        upsertedConcertsMap.put(concertId, scheduledConcert);
    }

    private Map<ConcertId, ScheduledConcert> loadMapFrom(ScheduledConcerts currentState) {
        Map<ConcertId, ScheduledConcert> availableConcertsMap = new HashMap<>();
        currentState.scheduledConcerts().forEach(concert -> availableConcertsMap.put(concert.concertId(), concert));
        return availableConcertsMap;
    }

}
