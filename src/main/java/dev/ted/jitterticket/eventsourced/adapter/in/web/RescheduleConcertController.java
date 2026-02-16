package dev.ted.jitterticket.eventsourced.adapter.in.web;

import dev.ted.jitterticket.eventsourced.application.port.EventStore;
import dev.ted.jitterticket.eventsourced.domain.concert.Concert;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@Controller
public class RescheduleConcertController {

    private final EventStore<ConcertId, ConcertEvent, Concert> concertEventStore;

    public RescheduleConcertController(EventStore<ConcertId, ConcertEvent, Concert> concertEventStore) {
        this.concertEventStore = concertEventStore;
    }

    @GetMapping("/reschedule/{concertId}")
    public String rescheduleConcertView(@PathVariable String concertId,
                                        Model model) {
        Concert concert = concertEventStore.findById(new ConcertId(UUID.fromString(concertId)))
                                           .orElseThrow(() -> new RuntimeException("Could not find concert with id: " + concertId));
        model.addAttribute("concert", ConcertView.from(concert));
        model.addAttribute("rescheduleForm", new RescheduleForm("", "", ""));
        return "reschedule-concert";
    }

    public record RescheduleForm(
            String newShowDate,
            String newShowTime,
            String newDoorsTime
    ) {
    }
}
