package dev.ted.jitterticket.eventsourced.adapter.in.web;

import dev.ted.jitterticket.eventsourced.application.ConcertSalesProjector;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;

@Controller
class SalesController {

    private final ConcertSalesProjector concertSummaryProjector;

    public SalesController(ConcertSalesProjector concertSummaryProjector) {
        this.concertSummaryProjector = concertSummaryProjector;
    }

    @GetMapping("/concert-sales")
    public String viewConcertSalesSummary(Model model) {
        List<ConcertSalesSummaryView> salesSummaryViews = concertSummaryProjector
                .allSalesSummaries()
                .map(concertSalesSummary ->
                             new ConcertSalesSummaryView(
                                     concertSalesSummary.concertId().id().toString(),
                                     concertSalesSummary.artist(),
                                     concertSalesSummary.showDateTime()
                                                        .toLocalDate()
                                                        .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)),
                                     String.valueOf(concertSalesSummary.totalQuantity()),
                                     "$" + concertSalesSummary.totalSales()
                             ))
                .toList();
        model.addAttribute("salesSummary", salesSummaryViews);
        return "concert-sales-view";
    }
}
