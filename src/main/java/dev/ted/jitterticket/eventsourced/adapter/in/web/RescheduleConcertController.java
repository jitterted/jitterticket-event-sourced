package dev.ted.jitterticket.eventsourced.adapter.in.web;

import dev.ted.jitterticket.eventsourced.application.CommandExecutorFactory;
import dev.ted.jitterticket.eventsourced.application.port.EventStore;
import dev.ted.jitterticket.eventsourced.domain.concert.Concert;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Controller
public class RescheduleConcertController {

    private final EventStore<ConcertId, ConcertEvent, Concert> concertEventStore;

    public RescheduleConcertController(EventStore<ConcertId, ConcertEvent, Concert> concertEventStore) {
        this.concertEventStore = concertEventStore;
    }

    @GetMapping("/reschedule/{concertId}")
    public String rescheduleConcertView(@PathVariable("concertId") String concertId,
                                        Model model) {
        Concert concert = concertOrThrow(new ConcertId(UUID.fromString(concertId)));
        model.addAttribute("concert", ConcertView.from(concert));
        model.addAttribute("rescheduleForm", RescheduleForm.from(concert));
        return "reschedule-concert";
    }

    @PostMapping("/reschedule/{concertId}")
    public String rescheduleConcert(@PathVariable("concertId") String concertId,
                                    RescheduleForm rescheduleForm) {
        CommandExecutorFactory commandExecutorFactory = CommandExecutorFactory.create(concertEventStore);
        var command = commandExecutorFactory.wrapWithParams(
                (concert, reschedule) ->
                        concert.rescheduleTo(
                                reschedule.showDateTime(),
                                reschedule.doorsTime()));
//        Reschedule rescheduleParams = new Reschedule(
//                rescheduleForm.newShowLocalDateTime(),
//                rescheduleForm.newDoorsLocalTime());
//        command.execute(new ConcertId(UUID.fromString(concertId)),
//                        rescheduleParams);

        return "redirect:/rescheduled/" + concertId;
    }

    private Concert concertOrThrow(ConcertId concertId1) {
        return concertEventStore.findById(concertId1)
                                .orElseThrow(() -> new RuntimeException("Could not find concert with id: " + concertId1.id()));
    }

    public record RescheduleForm(
            String newShowDate,
            String newShowTime,
            String newDoorsTime
    ) {
        public static RescheduleForm from(Concert concert) {
            return new RescheduleForm(
                    LocalDateTimeFormatting.extractFormattedDateFrom(concert.showDateTime()),
                    LocalDateTimeFormatting.extractFormattedTimeFrom(concert.showDateTime()),
                    LocalDateTimeFormatting.formatAsTimeFrom(concert.doorsTime())
            );
        }

        public LocalDateTime newShowLocalDateTime() {
            return LocalDateTimeFormatting.fromBrowserDateAndTime(
                    newShowDate, newShowTime);
        }

        public LocalTime newDoorsLocalTime() {
            return LocalTime.parse(newDoorsTime,
                    LocalDateTimeFormatting.HH_MM);
        }
    }
}
