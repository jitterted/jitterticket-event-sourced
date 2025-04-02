package dev.ted.jitterticket.eventsourced.adapter.out.store;

import dev.ted.jitterticket.eventsourced.domain.ConcertRescheduled;
import dev.ted.jitterticket.eventsourced.domain.ConcertScheduled;
import dev.ted.jitterticket.eventsourced.domain.CustomerRegistered;
import dev.ted.jitterticket.eventsourced.domain.Event;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Modifier;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;
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

    private Set<String> findAllConcreteEventClasses() {
        return findAllConcreteImplementations(Event.class)
                .stream()
                .map(Class::getSimpleName)
                .collect(Collectors.toSet());
    }

    private Set<Class<?>> findAllConcreteImplementations(Class<?> sealedInterface) {
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
                result.addAll(findAllConcreteImplementations(subclass));
            }
        }

        return result;
    }

    @ParameterizedTest
    @MethodSource("events")
    void eventRoundTripConversion(Event sourceEvent) {
        EventDto eventDto = EventDto.from(42L, 14, sourceEvent);

        Event actual = eventDto.toDomain();

        assertThat(actual)
                .isEqualTo(sourceEvent);
    }

    public static Stream<Arguments> events() {
        return Stream.of(
                Arguments.of(new ConcertScheduled("artist",
                                                  99,
                                                  LocalDateTime.now(),
                                                  LocalTime.now().minusHours(1),
                                                  100,
                                                  4))
                , Arguments.of(new ConcertRescheduled(LocalDateTime.now(),
                                                      LocalTime.now().minusHours(1)))
                , Arguments.of(new CustomerRegistered("customer name", "email@example.com"))
        );
    }
}