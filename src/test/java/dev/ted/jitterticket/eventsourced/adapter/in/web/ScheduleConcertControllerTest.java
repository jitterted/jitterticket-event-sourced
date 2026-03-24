package dev.ted.jitterticket.eventsourced.adapter.in.web;

import dev.ted.jitterticket.eventsourced.application.Checkpoint;
import dev.ted.jitterticket.eventsourced.application.LocalDateTimeFactory;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertFactory;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertScheduled;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

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

        String redirect = fixture.controller()
                                 .scheduleNewConcert(createScheduleForm(), new RedirectAttributesModelMap());

        assertThat(redirect)
                .isEqualTo("redirect:/concerts");
        assertThat(fixture.concertEventStore().allEventsAfter(Checkpoint.INITIAL))
                .hasExactlyElementsOfTypes(ConcertScheduled.class);
    }

    @Test
    void scheduleNewConcertThatConflictsWithExistingConcertRedirectsToForm() {
        var fixture = ScheduleConcertController.createForTest();
        LocalDateTime existingShowDateTime = LocalDateTimeFactory.withNow().oneMonthInTheFutureAtMidnight().plusHours(20);
        fixture.concertEventStore()
               .save(ConcertFactory.createConcertWithShowDateTimeOf(
                       ConcertId.createRandom(), existingShowDateTime));

        String showDate = existingShowDateTime.toLocalDate().format(DateTimeFormatter.ISO_DATE);
        ScheduleConcertController.ScheduleForm scheduleForm = createScheduleFormWith(
                showDate,
                existingShowDateTime.toLocalTime().format(DateTimeFormatter.ofPattern("hh:mm")));
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        String redirect = fixture.controller()
                                 .scheduleNewConcert(scheduleForm, redirectAttributes);

        assertThat(redirect)
                .isEqualTo("redirect:/schedule");
        assertThat((Map<String, String>) redirectAttributes.getFlashAttributes())
                .containsEntry("errorMessage", "Scheduling Conflict: a concert is already scheduled for " + showDate);
        assertThat(redirectAttributes.getFlashAttributes())
                .containsKey("scheduleForm");
    }

    @Test
    void scheduleFormDataNotReplacedIfAlreadyExistsInModel() {
        ScheduleConcertController scheduleConcertController = ScheduleConcertController.createForTest().controller();
        Model model = new ConcurrentModel();
        model.addAttribute("scheduleForm", createScheduleForm());

        scheduleConcertController.showScheduleForm(model);

        ScheduleConcertController.ScheduleForm scheduleForm = (ScheduleConcertController.ScheduleForm) model.getAttribute("scheduleForm");
        assertThat(scheduleForm.artist())
                .isEqualTo("Daylight Noise");

    }

    ScheduleConcertController.ScheduleForm createScheduleForm() {
        String showDate = "2026-03-14";
        String showTime = "19:00";
        return createScheduleFormWith(showDate, showTime);
    }

    ScheduleConcertController.ScheduleForm createScheduleFormWith(String showDate, String showTime) {
        return new ScheduleConcertController.ScheduleForm(
                "Daylight Noise",
                35,
                showDate,
                showTime,
                "18:00",
                75,
                2
        );
    }
}