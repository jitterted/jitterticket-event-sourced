package dev.ted.jitterticket.eventsourced.adapter.out.store;

import dev.ted.jitterticket.eventsourced.domain.Event;
import dev.ted.jitterticket.eventsourced.domain.TicketOrderId;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertRescheduled;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertScheduled;
import dev.ted.jitterticket.eventsourced.domain.concert.TicketsSold;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerEvent;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerId;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerRegistered;
import dev.ted.jitterticket.eventsourced.domain.customer.TicketsPurchased;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

class EventDtoTest {

    @ParameterizedTest
    @MethodSource("eventInterfaces")
    void allEventsAreTested(Class<?> eventInterface) {
        Set<String> concreteClassNames =
                Event.allConcreteImplementationsOf(eventInterface)
                     .stream()
                     .map(Class::getSimpleName)
                     .collect(Collectors.toSet());

        Set<String> eventClassesCoveredByParameterizedTest =
                events()
                        .map(arg -> arg.get()[0].getClass())
                        .map(Class::getSimpleName)
                        .collect(Collectors.toSet());

        assertThat(eventClassesCoveredByParameterizedTest)
                .as("Missing some Events from the parameterized test for " + eventInterface.getSimpleName())
                .containsAll(concreteClassNames);
    }

    @ParameterizedTest
    @MethodSource("eventInterfaces")
    void atLeastOneConcreteImplementationForEachEventInterface(Class<?> eventInterface) {
        assertThat(Event.allConcreteImplementationsOf(eventInterface))
                .as("No concrete implementations found for " + eventInterface.getSimpleName())
                .hasSizeGreaterThanOrEqualTo(1);
    }

    public static Stream<Arguments> eventInterfaces() {
        return Stream.of(
                Arguments.of(ConcertEvent.class),
                Arguments.of(CustomerEvent.class)
        );
    }

    @Test
    void uncommittedEventSerializedToJsonDoesNotContainEventSequence() {
        ConcertId concertId = new ConcertId(UUID.fromString("18a5ae4c-ecdb-429a-b3fe-2c1853fb37d2"));
        TicketsSold ticketsSold = new TicketsSold(concertId,
                                                  null, 6, 150);

        EventDto<TicketsSold> eventDto = EventDto.from(concertId.id(),
                                                   null,
                                                   ticketsSold);

        assertThat(eventDto.getJson())
                .isEqualTo(
                """
                {"concertId":{"id":"18a5ae4c-ecdb-429a-b3fe-2c1853fb37d2"},"quantity":6,"totalPaid":150}""");
    }

    @ParameterizedTest
    @MethodSource("events")
    void eventRoundTripConversion(Event sourceEvent) {
        long eventSequence = 17L;
        EventDto<Event> eventDto = EventDto.from(UUID.randomUUID(),
                                                 eventSequence,
                                                 sourceEvent);

        Event actual = eventDto.toDomain();

        assertThat(actual)
                .usingRecursiveComparison()
                .ignoringFields("eventSequence")
                .isEqualTo(sourceEvent);
        assertThat(actual.eventSequence())
                .as("Expected the eventSequence to be taken from the 'column' and not from the JSON")
                .isEqualTo(eventSequence);
    }

    public static Stream<Arguments> events() {
        return Stream.of(
                Arguments.of(new ConcertScheduled(ConcertId.createRandom(),
                                                  null,
                                                  "artist",
                                                  99,
                                                  LocalDateTime.now(),
                                                  LocalTime.now().minusHours(1),
                                                  100,
                                                  4))
                , Arguments.of(new ConcertRescheduled(ConcertId.createRandom(),
                                                      null,
                                                      LocalDateTime.now(),
                                                      LocalTime.now().minusHours(1)))
                , Arguments.of(new CustomerRegistered(CustomerId.createRandom(),
                                                      null,
                                                      "customer name",
                                                      "email@example.com"))
                , Arguments.of(new TicketsSold(ConcertId.createRandom(),
                                               null, 6, 150))
                , Arguments.of(new TicketsPurchased(
                        CustomerId.createRandom(),
                        null,
                        TicketOrderId.createRandom(),
                        ConcertId.createRandom(), 4, 100))
        );
    }

}
