package dev.ted.jitterticket.eventsourced.adapter.in.web;

import org.junit.jupiter.api.Test;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import static org.assertj.core.api.Assertions.*;

class SalesControllerTest {

    @Test
    void salesViewShowsSummaryOfSampleDataConcertSales() {
        SalesController controller = new SalesController(/* will need projector */);

        Model model = new ConcurrentModel();
        String viewName = controller.viewConcertSalesSummary(model);

        assertThat(viewName)
                .isEqualTo("concert-sales-view");
        assertThat(model.getAttribute("salesSummary"))
                .isNotNull();
    }
}