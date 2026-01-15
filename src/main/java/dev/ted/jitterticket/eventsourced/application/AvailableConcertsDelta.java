package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;

import java.util.List;

public record AvailableConcertsDelta(List<AvailableConcert> upsertedConcerts,
                                     List<ConcertId> removedConcertIds) {
// might need an "isEmpty" when we persist to the database
}
