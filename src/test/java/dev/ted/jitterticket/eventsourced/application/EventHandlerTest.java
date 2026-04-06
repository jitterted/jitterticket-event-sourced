package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.domain.Event;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertRescheduled;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertScheduled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

class EventHandlerTest {

    @Test
    void newEventConsumerDetectsHandledEventTypes() {
        SpyEventHandler handler = new SpyEventHandler();

        Set<Class<? extends Event>> handled = handler.handledEventTypes();

        assertThat(handled)
                .containsExactlyInAnyOrder(ConcertScheduled.class,
                                           ConcertRescheduled.class);
    }

    @Test
    void eventConsumerInvokesHandleMethodsPerEventType() {
        SpyEventHandler handler = new SpyEventHandler();
        Stream<ConcertEvent> stream =
                MakeEvents.with()
                          .concertScheduled(
                                  ConcertId.createRandom(),
                                  cust -> cust.rescheduleTo(
                                          LocalDateTimeFactory.withNow().oneMonthInTheFutureAtMidnight())
                          )
                          .stream();

        handler.handle(stream);

        assertThat(handler.handled())
                .containsExactlyInAnyOrder(
                        "Handled ConcertScheduled",
                        "Handled ConcertRescheduled");
    }

    @Test
    void exceptionThrownIfHandleStreamContainsUnwantedEvents() {
        SpyEventHandler handler = new SpyEventHandler();
        Stream<ConcertEvent> streamWithUnwantedEvents =
                MakeEvents.with()
                          .concertScheduled(ConcertId.createRandom(),
                                            cust ->
                                                    cust.rescheduleTo(LocalDateTimeFactory.withNow().oneMonthInTheFutureAtMidnight())
                                                        .ticketsSold(2)
                                                        .ticketsSold(4)
                                                        .ticketSalesStopped()
                          )
                          .stream();

        assertThatExceptionOfType(UnwantedEventException.class)
                .isThrownBy(() -> handler.handle(streamWithUnwantedEvents))
                .withMessage("Unwanted event class dev.ted.jitterticket.eventsourced.domain.concert.TicketsSold received, this class only accepts: ConcertRescheduled,ConcertScheduled");
    }
}

class SpyEventHandler extends EventHandler {

    private final List<String> handled = new ArrayList<>();

    public void handle(ConcertRescheduled concertRescheduled) {
        handled.add("Handled ConcertRescheduled");
    }

    public void handle(ConcertScheduled concertScheduled) {
        handled.add("Handled ConcertScheduled");
    }

    public List<String> handled() {
        return handled;
    }
}
