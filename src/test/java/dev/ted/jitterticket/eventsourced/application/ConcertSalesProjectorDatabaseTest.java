package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.adapter.out.store.jdbc.ConcertSalesProjection;
import dev.ted.jitterticket.eventsourced.adapter.out.store.jdbc.ConcertSalesProjectionRepository;
import dev.ted.jitterticket.eventsourced.adapter.out.store.jdbc.DataJdbcContainerTest;
import dev.ted.jitterticket.eventsourced.adapter.out.store.jdbc.ProjectionMetadata;
import dev.ted.jitterticket.eventsourced.adapter.out.store.jdbc.ProjectionMetadataRepository;
import dev.ted.jitterticket.eventsourced.application.port.EventStore;
import dev.ted.jitterticket.eventsourced.domain.EventSourcedAggregate;
import dev.ted.jitterticket.eventsourced.domain.Id;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

@SuppressWarnings("unchecked")
class ConcertSalesProjectorDatabaseTest extends DataJdbcContainerTest {

    @Autowired
    ConcertSalesProjectionRepository concertSalesProjectionRepository;

    @Autowired
    ProjectionMetadataRepository projectionMetadataRepository;

    @Test
    void newSalesProjectorSubscribesWithLastGlobalEventSequenceOfZero() {
        EventStoreSpy eventStoreSpy = new EventStoreSpy();

        ConcertSalesProjector concertSalesProjector =
                Projections.createForTest(eventStoreSpy,
                                          concertSalesProjectionRepository,
                                          projectionMetadataRepository);

        eventStoreSpy
                .assertSubscribeCalledWithLastGlobalSequenceOf(0);
    }

    @Test
    void subscribeWith9WhenLastGlobalSequenceInProjectionTableHas9() {
        EventStoreSpy eventStoreSpy = new EventStoreSpy();
        ProjectionMetadata projectionMetadata =
                new ProjectionMetadata(ConcertSalesProjector.PROJECTION_NAME,
                                       9L);
        projectionMetadataRepository.save(projectionMetadata);
        ConcertSalesProjector concertSalesProjector =
                Projections.createForTest(eventStoreSpy,
                                          concertSalesProjectionRepository,
                                          projectionMetadataRepository);

        eventStoreSpy
                .assertSubscribeCalledWithLastGlobalSequenceOf(9L);
    }

    @Nested
    class AllProjectedSummaries {

        @Test
        void loadsProjectionOfSingleConcertScheduledEventFromRepository() {
            ConcertId concertId = ConcertId.createRandom();
            String artist = "Artist";
            LocalDateTime showDateTime = LocalDate.now().atStartOfDay();
            concertSalesProjectionRepository.save(
                    new ConcertSalesProjection(
                            concertId.id(), artist,
                            showDateTime.toLocalDate(),
                            0, 0)
            );
            ProjectionMetadata projectionMetadata =
                    new ProjectionMetadata(ConcertSalesProjector.PROJECTION_NAME,
                                           1L);
            projectionMetadataRepository.save(projectionMetadata);
            ConcertSalesProjector concertSalesProjector =
                    Projections.createForTest(
                            projectionMetadataRepository,
                            concertSalesProjectionRepository
                            );

            Stream<ConcertSalesProjector.ConcertSalesSummary> allSalesSummaries =
                    concertSalesProjector.allSalesSummaries();

            assertThat(allSalesSummaries)
                    .containsExactly(
                            new ConcertSalesProjector.ConcertSalesSummary(
                                    concertId, artist,
                                    showDateTime,
                                    0, 0
                            )
                    );
        }
    }

    // == TEST DOUBLES ==

    @SuppressWarnings("rawtypes")
    static class EventStoreSpy implements EventStore {

        private boolean subscribeInvoked = false;
        private long subscribedLastGlobalEventSequence;

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
        public void subscribe(ConcertSalesProjector concertSalesProjector, long lastGlobalEventSequence) {
            subscribeInvoked = true;
            subscribedLastGlobalEventSequence = lastGlobalEventSequence;
        }

        public void assertSubscribeCalledWithLastGlobalSequenceOf(long expectedLastGlobalSequence) {
            assertThat(subscribeInvoked)
                    .as("Expected subscribe to be called")
                    .isTrue();
            assertThat(subscribedLastGlobalEventSequence)
                    .isEqualTo(expectedLastGlobalSequence);
        }

        @Override
        public Stream allEventsAfter(long globalEventSequence) {
            throw new UnsupportedOperationException();
        }
    }

}