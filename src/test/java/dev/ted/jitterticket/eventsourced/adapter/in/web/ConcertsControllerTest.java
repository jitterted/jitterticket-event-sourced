package dev.ted.jitterticket.eventsourced.adapter.in.web;

import dev.ted.jitterticket.eventsourced.application.AvailableConcertsProjector;
import dev.ted.jitterticket.eventsourced.application.ClockFactory;
import dev.ted.jitterticket.eventsourced.application.InMemoryEventStore;
import dev.ted.jitterticket.eventsourced.application.MemoryAvailableConcertsProjectionPersistence;
import dev.ted.jitterticket.eventsourced.application.ProjectionCoordinator;
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
        var concertStore = InMemoryEventStore.forConcerts();
        var concertId = ConcertId.createRandom();
        concertStore.save(ConcertFactory.scheduleConcertWith(
                concertId,
                "The Sonic Waves",
                45,
                LocalDateTime.of(2025, 7, 26, 20, 0),
                LocalTime.of(19, 0)));
        var projectionCoordinator = new ProjectionCoordinator<>(
                AvailableConcertsProjector.forTestWith(ClockFactory.fixedClockAt(2025, 7, 26)),
                new MemoryAvailableConcertsProjectionPersistence(),
                concertStore);

        ConcertsController concertsController = new ConcertsController(projectionCoordinator);

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