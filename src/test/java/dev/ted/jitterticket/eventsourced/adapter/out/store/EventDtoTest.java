package dev.ted.jitterticket.eventsourced.adapter.out.store;

import dev.ted.jitterticket.eventsourced.domain.Event;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertRescheduled;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertScheduled;
import dev.ted.jitterticket.eventsourced.domain.concert.TicketsBought;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerEvent;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerId;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerRegistered;
import org.junit.jupiter.api.Test;
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

    @Test
    void allEventsAreTested() {
        Set<String> allEventClasses = findAllConcreteEventClasses();

        Set<String> eventClassesCoveredByParameterizedTest = events()
                .map(arg -> arg.get()[0].getClass())
                .map(Class::getSimpleName)
                .collect(Collectors.toSet());

        assertThat(eventClassesCoveredByParameterizedTest)
                .as("Missing some Events from the parameterized test")
                .containsAll(allEventClasses);
    }

    @Test
    void atLeastOneConcreteImplementationForEachEventInterface() {
        assertThat(allConcreteImplementationsOf(ConcertEvent.class))
                .as("No concrete implementations found for ConcertEvent")
                .hasSizeGreaterThanOrEqualTo(1);

        assertThat(allConcreteImplementationsOf(CustomerEvent.class))
                .as("No concrete implementations found for CustomerEvent")
                .hasSizeGreaterThanOrEqualTo(1);
    }

    private Set<String> findAllConcreteEventClasses() {
        Set<Class<?>> allEventClasses = new HashSet<>();
        allEventClasses.addAll(allConcreteImplementationsOf(ConcertEvent.class));
        allEventClasses.addAll(allConcreteImplementationsOf(CustomerEvent.class));

        return allEventClasses.stream()
                .map(Class::getSimpleName)
                .collect(Collectors.toSet());
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
        EventDto<Event> eventDto = EventDto.from(UUID.randomUUID(), 14, sourceEvent);

        Event actual = eventDto.toDomain();

        assertThat(actual)
                .isEqualTo(sourceEvent);
    }

    public static Stream<Arguments> events() {
        return Stream.of(
                Arguments.of(new ConcertScheduled(ConcertId.createRandom(),
                                                  "artist",
                                                  99,
                                                  LocalDateTime.now(),
                                                  LocalTime.now().minusHours(1),
                                                  100,
                                                  4))
                , Arguments.of(new ConcertRescheduled(ConcertId.createRandom(),
                                                      LocalDateTime.now(),
                                                      LocalTime.now().minusHours(1)))
                , Arguments.of(new CustomerRegistered(CustomerId.createRandom(),
                                                      "customer name",
                                                      "email@example.com"))
                , Arguments.of(new TicketsBought(ConcertId.createRandom(),
                                                 CustomerId.createRandom(),
                                                 6))
        );
    }

}
