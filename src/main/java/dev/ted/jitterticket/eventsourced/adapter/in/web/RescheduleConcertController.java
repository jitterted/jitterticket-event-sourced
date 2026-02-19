package dev.ted.jitterticket.eventsourced.adapter.in.web;

import dev.ted.jitterticket.eventsourced.application.CommandWithParams;
import dev.ted.jitterticket.eventsourced.application.ConcertQuery;
import dev.ted.jitterticket.eventsourced.application.RescheduleParams;
import dev.ted.jitterticket.eventsourced.domain.concert.Concert;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Controller
public class RescheduleConcertController {

    private final ConcertQuery concertQuery;
    private final CommandWithParams<ConcertId, RescheduleParams> rescheduleCommand;

    public RescheduleConcertController(
            ConcertQuery concertQuery,
            CommandWithParams<ConcertId, RescheduleParams> rescheduleCommand) {
        this.concertQuery = concertQuery;
        this.rescheduleCommand = rescheduleCommand;
    }

    @GetMapping("/reschedule/{concertId}")
    public String rescheduleConcertView(@PathVariable("concertId") String concertId,
                                        Model model) {
        ConcertId id = ConcertId.from(concertId);
        Concert concert = concertQuery.find(id);
        model.addAttribute("concert", ConcertView.from(concert));
        model.addAttribute("rescheduleForm", RescheduleForm.from(concert));
        return "reschedule-concert";
    }

    @PostMapping("/reschedule/{concertId}")
    public String rescheduleConcert(@PathVariable("concertId") String concertId,
                                    RescheduleForm rescheduleForm) {

        rescheduleCommand.execute(ConcertId.from(concertId),
                                  rescheduleForm.asCommandParams());

        return "redirect:/reschedule/" + concertId;
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

        public RescheduleParams asCommandParams() {
            return new RescheduleParams(newShowLocalDateTime(), newDoorsLocalTime());
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
