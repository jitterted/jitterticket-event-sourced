package dev.ted.jitterticket.eventsourced.adapter.in.web;

import dev.ted.jitterticket.eventsourced.application.ConcertSummaryProjector;
import dev.ted.jitterticket.eventsourced.application.EventStore;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertFactory;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SuppressWarnings("unchecked")
class ConcertsControllerTest {

    @Test
    void ticketableConcertsAddedToModel() {
        var concertStore = EventStore.forConcerts();
        ConcertSummaryProjector concertSummaryProjector = new ConcertSummaryProjector(concertStore);
        ConcertId concertId = ConcertId.createRandom();
        concertStore.save(ConcertFactory.scheduleConcertWith(concertId,
                                                             "The Sonic Waves",
                                                             45,
                                                             LocalDateTime.of(2025, 7, 26, 20, 0),
                                                             LocalTime.of(19, 0)));

        ConcertsController concertsController = new ConcertsController(concertSummaryProjector);

        Model model = new ConcurrentModel();
        String viewName = concertsController.ticketableConcerts(model);

        assertThat(viewName)
                .isEqualTo("concerts");
        assertThat((List<ConcertView>) model.getAttribute("concerts"))
                .containsExactly(new ConcertView(
                        concertId.id().toString(),
                        "The Sonic Waves",
                        "$45",
                        "July 26, 2025",
                        "8:00\u202FPM")
                );
    }
}