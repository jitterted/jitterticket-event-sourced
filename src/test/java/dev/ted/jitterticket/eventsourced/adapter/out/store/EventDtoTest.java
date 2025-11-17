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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Modifier;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
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
                allConcreteImplementationsOf(eventInterface)
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
        assertThat(allConcreteImplementationsOf(eventInterface))
                .as("No concrete implementations found for " + eventInterface.getSimpleName())
                .hasSizeGreaterThanOrEqualTo(1);
    }

    public static Stream<Arguments> eventInterfaces() {
        return Stream.of(
                Arguments.of(ConcertEvent.class),
                Arguments.of(CustomerEvent.class)
        );
    }

    private Set<Class<?>> allConcreteImplementationsOf(Class<?> sealedInterface) {
        Set<Class<?>> result = new HashSet<>();

        if (!sealedInterface.isInterface() && !Modifier.isAbstract(sealedInterface.getModifiers())) {
            // This is a concrete class, add it
            result.add(sealedInterface);
            return result;
        }

        // Get direct permitted subclasses/interfaces
        Class<?>[] permittedSubclasses = sealedInterface.getPermittedSubclasses();
        if (permittedSubclasses == null) {
            return result;
        }

        // Recursively process each permitted subclass/interface
        for (Class<?> subclass : permittedSubclasses) {
            if (!subclass.isInterface() && !Modifier.isAbstract(subclass.getModifiers())) {
                // This is a concrete class, add it
                result.add(subclass);
            } else {
                // This is an interface or abstract class, recurse into it
                result.addAll(allConcreteImplementationsOf(subclass));
            }
        }

        return result;
    }

    @ParameterizedTest
    @MethodSource("events")
    void eventRoundTripConversion(Event sourceEvent) {
        EventDto<Event> eventDto = EventDto.from(UUID.randomUUID(),
                                                 14,
                                                 sourceEvent);

        Event actual = eventDto.toDomain();

        assertThat(actual)
                .isEqualTo(sourceEvent);
    }

    public static Stream<Arguments> events() {
        return Stream.of(
                Arguments.of(ConcertScheduled.createNew(
                        ConcertId.createRandom(),
                        0,
                        "artist",
                        99,
                        LocalDateTime.now(),
                        LocalTime.now().minusHours(1),
                        100,
                        4))
                , Arguments.of(ConcertRescheduled.createNew(
                        ConcertId.createRandom(),
                        0,
                        LocalDateTime.now(),
                        LocalTime.now().minusHours(1)))
                , Arguments.of(CustomerRegistered.createNew(
                        CustomerId.createRandom(),
                        0,
                        "customer name",
                        "email@example.com"))
                , Arguments.of(TicketsSold.createNew(
                        ConcertId.createRandom(),
                        0,
                        6,
                        42))
                , Arguments.of(TicketsPurchased.createNew(
                        CustomerId.createRandom(),
                        0,
                        TicketOrderId.createRandom(),
                        ConcertId.createRandom(),
                        4,
                        100))
        );
    }

}
