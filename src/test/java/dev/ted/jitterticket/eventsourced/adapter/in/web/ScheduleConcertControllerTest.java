package dev.ted.jitterticket.eventsourced.adapter.in.web;

import dev.ted.jitterticket.eventsourced.application.Checkpoint;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertScheduled;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import static org.assertj.core.api.Assertions.*;

class ScheduleConcertControllerTest {

    @Test
    void getShowScheduleFormLoadsDefaultForm() {
        ScheduleConcertController scheduleConcertController = ScheduleConcertController.createForTest().controller();

        Model model = new ConcurrentModel();
        scheduleConcertController.showScheduleForm(model);

        assertThat(model.getAttribute("scheduleForm"))
                .isNotNull()
                .isExactlyInstanceOf(ScheduleConcertController.ScheduleForm.class)
                .isEqualTo(new ScheduleConcertController.ScheduleForm(
                        "",
                        25,
                        "",
                        "20:00",
                        "19:00",
                        100,
                        8
                ));
    }

    @Test
    void postScheduleNewConcertCreatesConcert() {
        var fixture = ScheduleConcertController.createForTest();

        String redirect = fixture.controller().scheduleNewConcert(
                new ScheduleConcertController.ScheduleForm(
                        "Daylight Noise",
                        35,
                        "2026-03-14",
                        "19:00",
                        "18:00",
                        75,
                        2
                ));

        assertThat(redirect)
                .isEqualTo("redirect:/concerts");
        assertThat(fixture.concertEventStore().allEventsAfter(Checkpoint.INITIAL))
                .hasExactlyElementsOfTypes(ConcertScheduled.class);
    }

}