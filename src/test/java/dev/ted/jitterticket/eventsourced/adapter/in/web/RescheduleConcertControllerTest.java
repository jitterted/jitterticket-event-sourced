package dev.ted.jitterticket.eventsourced.adapter.in.web;

import dev.ted.jitterticket.eventsourced.application.InMemoryEventStore;
import dev.ted.jitterticket.eventsourced.application.LocalDateTimeFactory;
import dev.ted.jitterticket.eventsourced.domain.concert.Concert;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertFactory;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.*;

class RescheduleConcertControllerTest {

    @Test
    void rescheduleViewPopulatesModelWithRescheduleFormFromConcertId() {
        var concertStore = InMemoryEventStore.forConcerts();
        ConcertId concertId = ConcertId.createRandom();
        LocalDateTime showDateTime = LocalDateTimeFactory
                .withNow().oneWeekInTheFutureAtMidnight().plusHours(20);
        LocalTime doorsTime = LocalTime.of(19, 0);
        concertStore.save(ConcertFactory.createConcertWithShowAndDoors(
                concertId, showDateTime, doorsTime));
        String concertIdString = concertId.id().toString();

        RescheduleConcertController rescheduleConcertController = new RescheduleConcertController(concertStore);

        Model model = new ConcurrentModel();
        String viewName = rescheduleConcertController.rescheduleConcertView(concertIdString, model);

        assertThat(viewName)
                .isEqualTo("reschedule-concert");

        assertThat(model.getAttribute("concert"))
                .as("Expected the 'concert' to be in the model of type ConcertView")
                .isExactlyInstanceOf(ConcertView.class);

        RescheduleConcertController.RescheduleForm rescheduleForm = (RescheduleConcertController.RescheduleForm)
                model.getAttribute("rescheduleForm");
        assertThat(rescheduleForm)
                .as("Expected the RescheduleForm to be in the model named 'rescheduleForm'")
                .extracting("newShowDate", InstanceOfAssertFactories.STRING)
                .isNotBlank();
    }

    @Test
    void rescheduleFormFromConcertPrefillsPropertiesAsStrings() {
        LocalDateTime showDateTime = LocalDateTime.of(
                2026, 2, 16, 20, 0, 10, 10
        );
        LocalTime doorsTime = LocalTime.of(19, 0, 12, 12);
        Concert concert = ConcertFactory.createConcertWithShowAndDoors(showDateTime, doorsTime);

        var rescheduleForm = RescheduleConcertController.RescheduleForm.from(concert);

        assertThat(rescheduleForm)
                .isEqualTo(new RescheduleConcertController.RescheduleForm(
                        "2026-02-16",
                        "20:00",
                        "19:00"));
    }

}