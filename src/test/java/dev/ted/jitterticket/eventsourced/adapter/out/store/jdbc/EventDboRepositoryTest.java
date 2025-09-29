package dev.ted.jitterticket.eventsourced.adapter.out.store.jdbc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("AssertThatIsZeroOne")
@DataJdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class EventDboRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.6");

    @Autowired
    private EventDboRepository eventDboRepository;

    private UUID aggregateId1;
    private UUID aggregateId2;

    @BeforeEach
    void setUp() {
        aggregateId1 = UUID.randomUUID();
        aggregateId2 = UUID.randomUUID();
    }

    @Test
    void shouldSaveAndRetrieveEvent() {
        // Given
        String jsonPayload = "{\"orderId\":\"123\",\"amount\":99.99}";
        EventDbo event = new EventDbo(aggregateId1, 1, "OrderCreated", jsonPayload);

        // When
        eventDboRepository.save(event);
        // need to do a find to get database-filled columns (createdAt and globalSequence)
        List<EventDbo> retrieved = eventDboRepository.findByAggregateRootId(aggregateId1);

        // Then
        assertThat(retrieved).hasSize(1);
        EventDbo saved = retrieved.getFirst();

        // Then
        assertThat(saved).isNotNull();
        System.out.println(saved);
        assertThat(saved.getAggregateRootId()).isEqualTo(aggregateId1);
        assertThat(saved.getEventSequence()).isEqualTo(1);
        assertThat(saved.getEventType()).isEqualTo("OrderCreated");
        assertThat(saved.getJson()).isEqualTo(jsonPayload);
        assertThat(saved.getGlobalSequence()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getVersion()).isEqualTo(1);
    }

    @Test
    void shouldFindEventsByAggregateRootId() {
        // Given
        eventDboRepository.save(new EventDbo(aggregateId1, 1, "OrderCreated", "{\"orderId\":\"123\"}"));
        eventDboRepository.save(new EventDbo(aggregateId1, 2, "OrderPaid", "{\"orderId\":\"123\",\"amount\":99.99}"));
        eventDboRepository.save(new EventDbo(aggregateId1, 3, "OrderShipped", "{\"orderId\":\"123\",\"trackingId\":\"XYZ\"}"));
        eventDboRepository.save(new EventDbo(aggregateId2, 1, "OrderCreated", "{\"orderId\":\"456\"}"));

        // When
        List<EventDbo> events = eventDboRepository.findByAggregateRootId(aggregateId1);

        // Then
        assertThat(events).hasSize(3);
        assertThat(events.get(0).getEventSequence()).isEqualTo(1);
        assertThat(events.get(1).getEventSequence()).isEqualTo(2);
        assertThat(events.get(2).getEventSequence()).isEqualTo(3);
        assertThat(events).allMatch(e -> e.getAggregateRootId().equals(aggregateId1));
    }

    @Test
    void shouldFindAllEventsByGlobalSequence() {
        // Given
        eventDboRepository.save(new EventDbo(aggregateId1, 1, "OrderCreated", "{}"));
        eventDboRepository.save(new EventDbo(aggregateId2, 1, "OrderCreated", "{}"));
        eventDboRepository.save(new EventDbo(aggregateId1, 2, "OrderPaid", "{}"));

        // When
        List<EventDbo> events = eventDboRepository.findAllByGlobalSequence();

        // Then
        assertThat(events).hasSize(3);

        // Verify they're in global sequence order
        Long previousSequence = 0L;
        for (EventDbo event : events) {
            assertThat(event.getGlobalSequence()).isGreaterThan(previousSequence);
            previousSequence = event.getGlobalSequence();
        }
    }

    @Test
    void shouldFindEventsAfterGlobalSequence() {
        // Given
        eventDboRepository.save(new EventDbo(aggregateId1, 1, "OrderCreated", "{}"));
        eventDboRepository.save(new EventDbo(aggregateId1, 2, "OrderPaid", "{}"));
        eventDboRepository.save(new EventDbo(aggregateId2, 1, "OrderCreated", "{}"));

        // Retrieve all events to get their global sequences
        List<EventDbo> allEvents = eventDboRepository.findAllByGlobalSequence();
        assertThat(allEvents).hasSize(3);

        Long firstGlobalSequence = allEvents.getFirst().getGlobalSequence();

        // When
        List<EventDbo> eventsAfter = eventDboRepository.findEventsAfter(firstGlobalSequence);

        // Then
        assertThat(eventsAfter).hasSize(2);
        assertThat(eventsAfter.get(0).getGlobalSequence()).isGreaterThan(firstGlobalSequence);
        assertThat(eventsAfter.get(1).getGlobalSequence()).isGreaterThan(firstGlobalSequence);
    }

    @Test
    void shouldFindEventsByEventType() {
        // Given
        eventDboRepository.save(new EventDbo(aggregateId1, 1, "OrderCreated", "{}"));
        eventDboRepository.save(new EventDbo(aggregateId1, 2, "OrderPaid", "{}"));
        eventDboRepository.save(new EventDbo(aggregateId2, 1, "OrderCreated", "{}"));
        eventDboRepository.save(new EventDbo(aggregateId2, 2, "OrderShipped", "{}"));

        // When
        List<EventDbo> orderCreatedEvents = eventDboRepository.findByEventType("OrderCreated");

        // Then
        assertThat(orderCreatedEvents)
                .hasSize(2)
                .allMatch(e -> e.getEventType().equals("OrderCreated"));
    }

    @Test
    void shouldGetMaxEventSequenceForAggregate() {
        // Given
        eventDboRepository.save(new EventDbo(aggregateId1, 1, "OrderCreated", "{}"));
        eventDboRepository.save(new EventDbo(aggregateId1, 2, "OrderPaid", "{}"));
        eventDboRepository.save(new EventDbo(aggregateId1, 3, "OrderShipped", "{}"));

        // When
        Integer maxSequence = eventDboRepository.getMaxEventSequence(aggregateId1);

        // Then
        assertThat(maxSequence).isEqualTo(3);
    }

    @Test
    void shouldReturnZeroForMaxEventSequenceWhenNoEvents() {
        // When
        Integer maxSequence = eventDboRepository.getMaxEventSequence(UUID.randomUUID());

        // Then
        assertThat(maxSequence).isEqualTo(0);
    }

    @Test
    void shouldHandleComplexJsonPayload() {
        // Given
        String complexJson = """
                {
                    "orderId": "ORD-12345",
                    "customer": {
                        "id": "CUST-001",
                        "name": "John Doe",
                        "email": "john@example.com"
                    },
                    "items": [
                        {"sku": "ITEM-001", "quantity": 2, "price": 29.99},
                        {"sku": "ITEM-002", "quantity": 1, "price": 49.99}
                    ],
                    "total": 109.97,
                    "metadata": {
                        "source": "web",
                        "campaign": "summer-sale"
                    }
                }
                """;
        EventDbo event = new EventDbo(aggregateId1, 1, "OrderCreated", complexJson);

        // When
        /*EventDbo saved = */eventDboRepository.save(event);
        List<EventDbo> retrieved = eventDboRepository.findByAggregateRootId(aggregateId1);

        // Then
        assertThat(retrieved).hasSize(1);
        assertThat(retrieved.getFirst().getJson()).isEqualTo(complexJson);
    }

    @Test
    void shouldMaintainEventOrderingWithinAggregate() {
        // Given - Save events out of order
        eventDboRepository.save(new EventDbo(aggregateId1, 3, "OrderShipped", "{}"));
        eventDboRepository.save(new EventDbo(aggregateId1, 1, "OrderCreated", "{}"));
        eventDboRepository.save(new EventDbo(aggregateId1, 2, "OrderPaid", "{}"));

        // When
        List<EventDbo> events = eventDboRepository.findByAggregateRootId(aggregateId1);

        // Then - Should be returned in sequence order
        assertThat(events).hasSize(3);
        assertThat(events).extracting(EventDbo::getEventSequence)
                .containsExactly(1, 2, 3);
    }
}