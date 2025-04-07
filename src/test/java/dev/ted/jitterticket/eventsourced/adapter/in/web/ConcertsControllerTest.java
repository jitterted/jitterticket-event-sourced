package dev.ted.jitterticket.eventsourced.adapter.in.web;

import org.junit.jupiter.api.Test;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import static org.assertj.core.api.Assertions.*;

class ConcertsControllerTest {

    @Test
    void ticketableConcertsAddedToModel() {
        ConcertsController concertsController = new ConcertsController();

        Model model = new ConcurrentModel();
        String viewName = concertsController.ticketableConcerts(model);

        assertThat(viewName)
                .isEqualTo("concerts");
        assertThat(model.getAttribute("concerts"))
                .isNotNull();
    }
}