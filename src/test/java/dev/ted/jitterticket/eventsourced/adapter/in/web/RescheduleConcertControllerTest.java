package dev.ted.jitterticket.eventsourced.adapter.in.web;

import dev.ted.jitterticket.eventsourced.application.CommandExecutorFactory;
import dev.ted.jitterticket.eventsourced.application.Commands;
import dev.ted.jitterticket.eventsourced.application.ConcertQuery;
import dev.ted.jitterticket.eventsourced.application.InMemoryEventStore;
import dev.ted.jitterticket.eventsourced.application.LocalDateTimeFactory;
import dev.ted.jitterticket.eventsourced.application.port.EventStore;
import dev.ted.jitterticket.eventsourced.domain.concert.Concert;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
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
        RescheduleConcertController rescheduleConcertController = createController(concertStore);

        Model model = new ConcurrentModel();
        String viewName = rescheduleConcertController.rescheduleConcertView(concertId.id().toString(), model);

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

    private static RescheduleConcertController createController(EventStore<ConcertId, ConcertEvent, Concert> concertStore) {
        return new RescheduleConcertController(
                new ConcertQuery(concertStore),
                new Commands(CommandExecutorFactory.create(concertStore)).createRescheduleCommand());
    }

    @Test
    void postToRescheduleConcertDoesRescheduleAndRedirectsToRescheduledView() {
        var concertStore = InMemoryEventStore.forConcerts();
        ConcertId concertId = ConcertId.createRandom();
        LocalDateTime showDateTime = LocalDateTime.of(2026, 2, 16, 21, 30);
        LocalTime doorsTime = LocalTime.of(20, 30);
        concertStore.save(ConcertFactory.createConcertWithShowAndDoors(
                concertId, showDateTime, doorsTime));
        RescheduleConcertController rescheduleConcertController = createController(concertStore);
        String concertIdString = concertId.id().toString();

        String redirect = rescheduleConcertController
                .rescheduleConcert(concertIdString, rescheduleFormOf(
                        "2026-03-14", "21:00", "20:00"));

        assertThat(redirect)
                .isEqualTo("redirect:/reschedule/" + concertIdString);
        assertThat(concertStore.findById(concertId))
                .get()
                .extracting(Concert::showDateTime, Concert::doorsTime)
                .containsExactly(LocalDateTime.of(2026, 3, 14, 21, 0),
                                 LocalTime.of(20, 0));
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

    @Test
    void formConvertsStringsToLocalDateTimeAndLocalTime() {
        var rescheduleForm = rescheduleFormOf("2026-03-14", "21:00", "20:00");

        assertThat(rescheduleForm.newDoorsLocalTime())
                .isEqualTo(LocalTime.of(20, 0));
        assertThat(rescheduleForm.newShowLocalDateTime())
                .isEqualTo(LocalDateTime.of(2026, 3, 14, 21, 0));
    }

    private static RescheduleConcertController.RescheduleForm rescheduleFormOf(String newShowDate, String newShowTime, String newDoorsTime) {
        return new RescheduleConcertController.RescheduleForm(
                newShowDate,
                newShowTime,
                newDoorsTime);
    }
}