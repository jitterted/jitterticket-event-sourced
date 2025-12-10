package dev.ted.jitterticket.eventsourced.adapter.in.web;

import dev.ted.jitterticket.eventsourced.application.ConcertSalesProjectionMediator;
import dev.ted.jitterticket.eventsourced.application.ConcertSalesProjector;
import dev.ted.jitterticket.eventsourced.application.InMemoryEventStore;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertFactory;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SuppressWarnings("unchecked")
class SalesControllerTest {

    @Disabled("Until we've completed the separation of concerns in ConcertSalesProjector")
    @Test
    void salesViewShowsSummaryOfSampleDataConcertSales() {
        var concertStore = InMemoryEventStore.forConcerts();
        ConcertSalesProjectionMediator concertSalesProjectionMediator = new ConcertSalesProjectionMediator(new ConcertSalesProjector(),
                                                                                                           concertStore,
                                                                                                           null, null);

        ConcertId concertId = ConcertId.createRandom();
        concertStore.save(ConcertFactory.scheduleConcertWith(
                concertId,
                "White Label Community",
                25,
                LocalDateTime.of(2025, 12, 26, 19, 30),
                LocalTime.of(18, 30)));

        SalesController controller = new SalesController(concertSalesProjectionMediator);

        Model model = new ConcurrentModel();
        String viewName = controller.viewConcertSalesSummary(model);

        assertThat(viewName)
                .isEqualTo("concert-sales-view");
        assertThat((List<ConcertSalesSummaryView>) model.getAttribute("salesSummaryViews"))
                .containsExactly(new ConcertSalesSummaryView(
                        concertId.id().toString(),
                        "White Label Community",
                        "December 26, 2025",
                        "0",
                        "$0"
                ));
    }
}