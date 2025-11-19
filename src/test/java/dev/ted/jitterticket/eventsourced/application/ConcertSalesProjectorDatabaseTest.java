package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.adapter.out.store.jdbc.ConcertSalesProjectionRepository;
import dev.ted.jitterticket.eventsourced.adapter.out.store.jdbc.DataJdbcContainerTest;
import dev.ted.jitterticket.eventsourced.adapter.out.store.jdbc.ProjectionRepository;
import dev.ted.jitterticket.eventsourced.application.port.EventStore;
import dev.ted.jitterticket.eventsourced.domain.EventSourcedAggregate;
import dev.ted.jitterticket.eventsourced.domain.Id;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

class ConcertSalesProjectorDatabaseTest extends DataJdbcContainerTest {

    @Autowired
    ConcertSalesProjectionRepository concertSalesProjectionRepository;

    @Autowired
    ProjectionRepository projectionRepository;

    @Test
    void newSalesProjectorSubscribesWithLastGlobalEventSequenceOfZero() {
        // Spy for the EventStore with an override for the subscribe method:
        // - we want to verify that the lastGlobalEventSequence passed into the subscribe is 0


    }

    static class EventStoreSpy implements EventStore {

        @Override
        public void save(EventSourcedAggregate aggregate) {}

        @Override
        public void save(Id aggregateId, Stream uncommittedEvents) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional findById(Id id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Stream allEvents() {
            throw new UnsupportedOperationException();
        }

        @Override
        public List eventsForAggregate(Id id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void subscribe(ConcertSalesProjector concertSalesProjector /*, lastGlobalEventSequence */) {
            // hold onto the (parameter to add:) lastGlobalEventSequence
        }
    }

}