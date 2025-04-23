package dev.ted.jitterticket.eventsourced.adapter.in.web;

import dev.ted.jitterticket.eventsourced.TixConfiguration;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@Tag("mvc")
@Tag("spring")
@WebMvcTest(CustomersController.class)
@Import(TixConfiguration.class)
class CustomersControllerMvcTest {

    @Autowired
    MockMvcTester mvc;

    @Test
    void getToCustomerTicketPurchaseConfirmationReturns200Ok() {
        mvc.get()
           .uri("/customers/asdf/confirmations/qwerty")
           .assertThat()
           .hasStatus2xxSuccessful();
    }
}