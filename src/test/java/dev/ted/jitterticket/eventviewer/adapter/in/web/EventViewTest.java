package dev.ted.jitterticket.eventviewer.adapter.in.web;

import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertRescheduled;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.*;

class EventViewTest {

    @Test
    void typeNameContainsClassSimpleName() {
        ConcertRescheduled event = ConcertRescheduled.createNew(
                ConcertId.createRandom(), 7,
                LocalDateTime.now(),
                LocalTime.now());

        EventView eventView = EventView.of(event);

        assertThat(eventView.eventName())
                .isEqualTo("ConcertRescheduled");
    }

    @Test
    void fieldsContainsAllEventRecordComponentsExceptForAggregateId() {
        ConcertId concertId = ConcertId.createRandom();
        LocalTime newDoorsTime = LocalTime.now();
        LocalDateTime newShowDateTime = LocalDateTime.now();
        ConcertRescheduled event = ConcertRescheduled.createNew(
                concertId, 5,
                newShowDateTime,
                newDoorsTime);

        EventView eventView = EventView.of(event);

        assertThat(eventView.fields())
                .containsExactlyInAnyOrder(
                        new EventView.FieldView("newShowDateTime", newShowDateTime.toString()),
                        new EventView.FieldView("newDoorsTime", newDoorsTime.toString())
                );

    }
}