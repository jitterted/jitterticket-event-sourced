package dev.ted.jitterticket.eventsourced.adapter.in.web;

import dev.ted.jitterticket.eventsourced.application.CommandExecutorFactory;
import dev.ted.jitterticket.eventsourced.application.Commands;
import dev.ted.jitterticket.eventsourced.application.CreateWithParams;
import dev.ted.jitterticket.eventsourced.application.InMemoryEventStore;
import dev.ted.jitterticket.eventsourced.application.ScheduleParams;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.time.LocalTime;

@Controller
class ScheduleConcertController {

    private final CreateWithParams<ConcertId, ScheduleParams> scheduleCommand;

    public ScheduleConcertController(CreateWithParams<ConcertId, ScheduleParams> scheduleCommand) {
        this.scheduleCommand = scheduleCommand;
    }

    static Fixture createForTest() {
        var concertEventStore = InMemoryEventStore.forConcerts();
        ScheduleConcertController scheduleConcertController =
                new ScheduleConcertController(new Commands(CommandExecutorFactory.create(concertEventStore)).createScheduleCommand());
        return new Fixture(concertEventStore, scheduleConcertController);
    }

    @GetMapping("/schedule")
    public String showScheduleForm(Model model) {
        model.addAttribute("scheduleForm", new ScheduleForm(
                "",
                25,
                "",
                "20:00",
                "19:00",
                100,
                8
        ));
        return "schedule-concert";
    }

    @PostMapping("/schedule")
    public String scheduleNewConcert(ScheduleForm scheduleForm) {
        scheduleCommand.execute(scheduleForm.toParams());
        return "redirect:/concerts";
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
                    LocalTime.parse(doorsTime, LocalDateTimeFormatting.HH_MM),
                    maxCapacity,
                    maxPerPurchase);
        }

    }


    record Fixture(
            dev.ted.jitterticket.eventsourced.application.port.EventStore<ConcertId, dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent, dev.ted.jitterticket.eventsourced.domain.concert.Concert> concertEventStore,
            ScheduleConcertController controller) {}
}
