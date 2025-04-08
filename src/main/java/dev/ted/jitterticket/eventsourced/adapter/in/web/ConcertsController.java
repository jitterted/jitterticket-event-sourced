package dev.ted.jitterticket.eventsourced.adapter.in.web;

import dev.ted.jitterticket.eventsourced.application.ConcertProjector;
import dev.ted.jitterticket.eventsourced.application.ConcertTicketView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.function.Function;

@Controller
public class ConcertsController {

    private final ConcertProjector concertProjector;

    @Autowired
    public ConcertsController(ConcertProjector concertProjector) {
        this.concertProjector = concertProjector;
    }

    @GetMapping("/concerts")
    public String ticketableConcerts(Model model) {
        List<ConcertView> concertViews =
                concertProjector.allConcertTicketViews()
                                .map(toConcertView())
                                .toList();
        model.addAttribute("concerts", concertViews);
        return "concerts";
    }

    private Function<ConcertTicketView, ConcertView> toConcertView() {
        return concertTicketView ->
        {
            String showDate = concertTicketView.showDateTime()
                                               .toLocalDate()
                                               .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG));
            String showTime = concertTicketView.showDateTime()
                                               .toLocalTime()
                                               .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT));
            String ticketPrice = "$" + concertTicketView.ticketPrice();

            return new ConcertView(concertTicketView.concertId().id().toString(),
                                   concertTicketView.artist(),
                                   ticketPrice,
                                   showDate,
                                   showTime);
        };
    }

}
