package dev.ted.jitterticket.eventsourced.adapter.in.web;

import dev.ted.jitterticket.eventsourced.application.ConcertSalesProjectionMediator;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
class SalesController {

    private final ConcertSalesProjectionMediator concertSalesProjectionMediator;

    public SalesController(ConcertSalesProjectionMediator concertSalesProjectionMediator) {
        this.concertSalesProjectionMediator = concertSalesProjectionMediator;
    }

    @GetMapping("/concert-sales")
    public String viewConcertSalesSummary(Model model) {
        List<ConcertSalesSummaryView> salesSummaryViews = concertSalesProjectionMediator
                .allSalesSummaries()
                .map(ConcertSalesSummaryView::from)
                .toList();
        model.addAttribute("salesSummaryViews", salesSummaryViews);
        return "concert-sales-view";
    }

}
