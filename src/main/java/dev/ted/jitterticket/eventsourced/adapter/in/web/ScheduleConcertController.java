package dev.ted.jitterticket.eventsourced.adapter.in.web;

import dev.ted.jitterticket.eventsourced.application.CommandExecutorFactory;
import dev.ted.jitterticket.eventsourced.application.Commands;
import dev.ted.jitterticket.eventsourced.application.CreateWithParams;
import dev.ted.jitterticket.eventsourced.application.InMemoryEventStore;
import dev.ted.jitterticket.eventsourced.application.MemoryScheduledConcertsProjectionPersistence;
import dev.ted.jitterticket.eventsourced.application.ProjectionCoordinator;
import dev.ted.jitterticket.eventsourced.application.ScheduleParams;
import dev.ted.jitterticket.eventsourced.application.ScheduledConcertsProjector;
import dev.ted.jitterticket.eventsourced.application.SchedulingConflictException;
import dev.ted.jitterticket.eventsourced.application.port.EventStore;
import dev.ted.jitterticket.eventsourced.domain.concert.Concert;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalTime;

@Controller
class ScheduleConcertController {

    private final CreateWithParams<ConcertId, ScheduleParams> scheduleCommand;

    public ScheduleConcertController(CreateWithParams<ConcertId, ScheduleParams> scheduleCommand) {
        this.scheduleCommand = scheduleCommand;
    }

    static Fixture createForTest() {
        var concertEventStore = InMemoryEventStore.forConcerts();
        var projectionCoordinator = new ProjectionCoordinator<>(
                new ScheduledConcertsProjector(),
                new MemoryScheduledConcertsProjectionPersistence(),
                concertEventStore);
        Commands commands = new Commands(
                CommandExecutorFactory.create(concertEventStore),
                projectionCoordinator);
        ScheduleConcertController scheduleConcertController =
                new ScheduleConcertController(commands.createScheduleCommand());
        return new Fixture(concertEventStore, scheduleConcertController);
    }

    record Fixture(
            EventStore<ConcertId, ConcertEvent, Concert> concertEventStore,
            ScheduleConcertController controller) {}


    @GetMapping("/schedule")
    public String showScheduleForm(Model model) {
        if (!model.containsAttribute("scheduleForm")) {
            model.addAttribute("scheduleForm", new ScheduleForm(
                    "",
                    25,
                    "",
                    "20:00",
                    "19:00",
                    100,
                    8
            ));
        }
        return "schedule-concert";
    }

    @PostMapping("/schedule")
    public String scheduleNewConcert(ScheduleForm scheduleForm,
                                     RedirectAttributes redirectAttributes) {
        try {
            scheduleCommand.execute(scheduleForm.toParams());
            return "redirect:/concerts";
        } catch (SchedulingConflictException sce) {
            redirectAttributes.addFlashAttribute("errorMessage", sce.getMessage());
            redirectAttributes.addFlashAttribute("scheduleForm", scheduleForm);
            return "redirect:/schedule";
        }
    }

    record ScheduleForm(String artist,
                        int ticketPrice,
                        String showDate,
                        String showTime,
                        String doorsTime,
                        int maxCapacity,
                        int maxPerPurchase) {

        ScheduleParams toParams() {
            return new ScheduleParams(
                    artist,
                    ticketPrice,
                    LocalDateTimeFormatting.fromBrowserDateAndTime(showDate, showTime),
                    LocalTime.parse(doorsTime, LocalDateTimeFormatting.HH_MM_24_HOUR_FORMAT),
                    maxCapacity,
                    maxPerPurchase);
        }

    }


}
