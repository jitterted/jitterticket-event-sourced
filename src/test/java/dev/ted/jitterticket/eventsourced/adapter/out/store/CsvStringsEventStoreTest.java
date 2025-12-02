package dev.ted.jitterticket.eventsourced.adapter.out.store;

import dev.ted.jitterticket.Gatherers;
import dev.ted.jitterticket.eventsourced.domain.Event;
import dev.ted.jitterticket.eventsourced.domain.concert.Concert;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertRescheduled;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertScheduled;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerId;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerRegistered;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

@SuppressWarnings("SpellCheckingInspection")
class CsvStringsEventStoreTest {

    @Test
    void saveStoresEventsAsCsvLines() {
        ArrayListStringsReaderAppender stringsReaderAppender = new ArrayListStringsReaderAppender();
        var concertEventStore = (CsvStringsEventStore<ConcertId, ConcertEvent, Concert>) CsvStringsEventStore.forConcerts(stringsReaderAppender);
        ConcertId concertId = new ConcertId(UUID.fromString("a087bb99-b920-41d9-a25b-d96ac779be0b"));
        LocalDateTime originalShowDateTime = LocalDateTime.of(2025, 10, 10, 20, 0);
        Concert concert = Concert.schedule(concertId,
                                           "Name of Artist",
                                           99,
                                           originalShowDateTime,
                                           originalShowDateTime.toLocalTime().minusHours(1),
                                           100,
                                           4);
        LocalDateTime newShowDateTime = originalShowDateTime.plusMonths(1);
        concert.rescheduleTo(newShowDateTime, newShowDateTime.toLocalTime().minusHours(1));

        concertEventStore.save(concert);

        assertThat(stringsReaderAppender.readAllLines())
                .hasSize(2)
                .containsExactly("a087bb99-b920-41d9-a25b-d96ac779be0b,0,1,dev.ted.jitterticket.eventsourced.domain.concert.ConcertScheduled,{\"concertId\":{\"id\":\"a087bb99-b920-41d9-a25b-d96ac779be0b\"},\"eventSequence\":0,\"artist\":\"Name of Artist\",\"ticketPrice\":99,\"showDateTime\":[2025,10,10,20,0],\"doorsTime\":[19,0],\"capacity\":100,\"maxTicketsPerPurchase\":4}",
                                 "a087bb99-b920-41d9-a25b-d96ac779be0b,1,2,dev.ted.jitterticket.eventsourced.domain.concert.ConcertRescheduled,{\"concertId\":{\"id\":\"a087bb99-b920-41d9-a25b-d96ac779be0b\"},\"eventSequence\":1,\"newShowDateTime\":[2025,11,10,20,0],\"newDoorsTime\":[19,0]}");
    }

    @Test
    void saveThatAppendsToExistingCsvGeneratesCorrectGlobalEventSequence() {
        // TODO: convert setup to use MakeEvents instead of creating aggregates
        ArrayListStringsReaderAppender stringsReaderAppender =
                new ArrayListStringsReaderAppender();
        ConcertId concertId = new ConcertId(UUID.fromString("a087bb99-b920-41d9-a25b-d96ac779be0b"));
        LocalDateTime originalShowDateTime = LocalDateTime.of(2025, 10, 10, 20, 0);
        ConcertScheduled concertScheduled = new ConcertScheduled(concertId,
                                                                 0,
                                                                 "Name of Artist",
                                                                 99,
                                                                 originalShowDateTime,
                                                                 originalShowDateTime.toLocalTime().minusHours(1),
                                                                 100,
                                                                 4);
        // save in new event store instance
        CsvStringsEventStore.forConcerts(stringsReaderAppender)
                            .save(concertId, Stream.of(concertScheduled));

        LocalDateTime newShowDateTime = originalShowDateTime.plusMonths(1);
        ConcertRescheduled concertRescheduled = new ConcertRescheduled(
                concertId, 1, newShowDateTime, newShowDateTime.toLocalTime().minusHours(1));

        // save new event in a new event store instance
        CsvStringsEventStore.forConcerts(stringsReaderAppender)
                            .save(concertId, Stream.of(concertRescheduled));

        assertThat(stringsReaderAppender.readAllLines())
                .hasSize(2)
                .containsExactly("a087bb99-b920-41d9-a25b-d96ac779be0b,0,1,dev.ted.jitterticket.eventsourced.domain.concert.ConcertScheduled,{\"concertId\":{\"id\":\"a087bb99-b920-41d9-a25b-d96ac779be0b\"},\"eventSequence\":0,\"artist\":\"Name of Artist\",\"ticketPrice\":99,\"showDateTime\":[2025,10,10,20,0],\"doorsTime\":[19,0],\"capacity\":100,\"maxTicketsPerPurchase\":4}",
                                 "a087bb99-b920-41d9-a25b-d96ac779be0b,1,2,dev.ted.jitterticket.eventsourced.domain.concert.ConcertRescheduled,{\"concertId\":{\"id\":\"a087bb99-b920-41d9-a25b-d96ac779be0b\"},\"eventSequence\":1,\"newShowDateTime\":[2025,11,10,20,0],\"newDoorsTime\":[19,0]}");

    }

    @Test
    void eventsForAggregateReturnsCorrectEventInstances() {
        ArrayListStringsReaderAppender stringsReaderAppender = new ArrayListStringsReaderAppender();
        var concertEventStore = (CsvStringsEventStore<ConcertId, ConcertEvent, Concert>) CsvStringsEventStore.forConcerts(stringsReaderAppender);
        String concertUuidString = "a087bb99-b920-41d9-a25b-d96ac779be0b";

        stringsReaderAppender.appendLines(
                List.of(
                        concertUuidString + ",0,1,dev.ted.jitterticket.eventsourced.domain.concert.ConcertScheduled,{\"concertId\":{\"id\":\"a087bb99-b920-41d9-a25b-d96ac779be0b\"},\"eventSequence\":0,\"artist\":\"Name of Artist\",\"ticketPrice\":99,\"showDateTime\":[2025,10,10,20,0],\"doorsTime\":[19,0],\"capacity\":100,\"maxTicketsPerPurchase\":4}",
                        concertUuidString + ",1,2,dev.ted.jitterticket.eventsourced.domain.concert.ConcertRescheduled,{\"concertId\":{\"id\":\"a087bb99-b920-41d9-a25b-d96ac779be0b\"},\"eventSequence\":1,\"newShowDateTime\":[2025,11,10,20,0],\"newDoorsTime\":[19,0]}"
                ));

        List<ConcertEvent> concertEvents = concertEventStore.eventsForAggregate(
                new ConcertId(UUID.fromString(concertUuidString))
        );

        assertThat(concertEvents)
                .hasSize(2)
                .hasOnlyElementsOfTypes(ConcertScheduled.class, ConcertRescheduled.class);
    }

    @Test
    void findReconstitutesMatchingAggregateFromCsvLines() {
        ArrayListStringsReaderAppender stringsReaderAppender = new ArrayListStringsReaderAppender();
        CsvStringsEventStore<ConcertId, ConcertEvent, Concert> concertEventStore = (CsvStringsEventStore<ConcertId, ConcertEvent, Concert>) CsvStringsEventStore.forConcerts(stringsReaderAppender);
        String concertUuidString = "a087bb99-b920-41d9-a25b-d96ac779be0b";
        String otherConcertUuidString = UUID.randomUUID().toString();
        stringsReaderAppender.appendLines(List.of(
                concertUuidString + ",0,1,dev.ted.jitterticket.eventsourced.domain.concert.ConcertScheduled,{\"concertId\":{\"id\":\"" + concertUuidString + "\"},\"eventSequence\":0,\"artist\":\"Name of Artist\",\"ticketPrice\":99,\"showDateTime\":[2025,10,10,20,0],\"doorsTime\":[19,0],\"capacity\":100,\"maxTicketsPerPurchase\":4}",
                otherConcertUuidString + ",0,2,dev.ted.jitterticket.eventsourced.domain.concert.ConcertScheduled,{\"concertId\":{\"id\":\"" + otherConcertUuidString + "\"},\"eventSequence\":0,\"artist\":\"Other Concert - Should Not Be Returned\",\"ticketPrice\":99,\"showDateTime\":[2025,10,10,20,0],\"doorsTime\":[19,0],\"capacity\":100,\"maxTicketsPerPurchase\":4}",
                concertUuidString + ",1,3,dev.ted.jitterticket.eventsourced.domain.concert.ConcertRescheduled,{\"concertId\":{\"id\":\"" + concertUuidString + "\"},\"eventSequence\":1,\"newShowDateTime\":[2025,11,10,20,0],\"newDoorsTime\":[19,0]}"));

        Concert concert = concertEventStore.findById(new ConcertId(UUID.fromString(concertUuidString)))
                                           .orElseThrow();

        assertThat(concert.artist())
                .isEqualTo("Name of Artist");
        assertThat(concert.ticketPrice())
                .isEqualTo(99);
    }

    @Test
    void saveAndReconstituteEvents() throws ClassNotFoundException {
        ConcertId concertId = ConcertId.createRandom();
        LocalDateTime newShowDateTime = LocalDateTime.now().plusMonths(1);
        List<ConcertEvent> concertEvents = List.of(
                new ConcertScheduled(concertId,
                                     0,
                                     "Name of Artist",
                                     99,
                                     LocalDateTime.now(),
                                     LocalTime.now().minusHours(1),
                                     100,
                                     4)
                , new ConcertRescheduled(concertId,
                                         1,
                                         newShowDateTime,
                                         newShowDateTime.toLocalTime().minusHours(1))
        );

        AtomicLong globalEventSequence = new AtomicLong(1L);
        List<EventDto<ConcertEvent>> eventDtoList =
                concertEvents.stream()
                             .map(event -> EventDto.from(
                                     concertId.id(),
                                     event.eventSequence(),
                                     globalEventSequence.getAndIncrement(),
                                     event))
                             .toList();

        List<String> csvEventDtoRecords = eventDtoList
                .stream()
                .map(CsvStringsEventStore::toCsv)
                .toList();

        CsvStringsEventStore<ConcertId, ConcertEvent, Concert> concertEventStore = (CsvStringsEventStore<ConcertId, ConcertEvent, Concert>) CsvStringsEventStore.forConcerts(new ArrayListStringsReaderAppender());
        List<? extends EventDto<? extends Event>> eventDtos = csvEventDtoRecords
                .stream()
                .map(concertEventStore::csvToEventDto)
                .toList();

        Class<?> eventClass = Class.forName("dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent");
        List<?> events = eventDtos
                .stream()
                .map(EventDto::toDomain)
                .gather(Gatherers.castTo(eventClass))
                .toList();

        @SuppressWarnings("unchecked")
        Concert concert = Concert.reconstitute((List<ConcertEvent>) events);
        assertThat(concert.artist())
                .isEqualTo("Name of Artist");
        assertThat(concert.showDateTime())
                .isEqualTo(newShowDateTime);
    }

    @Test
    void eventDtoConvertedToAndFromCsvCorrectly() {
        String customerUuid = "3ed83b5e-9564-42a5-8329-3cc5d5e14840";
        CustomerRegistered event = new CustomerRegistered(new CustomerId(UUID.fromString(customerUuid)),
                                                          0,
                                                          "Customer Name",
                                                          "customer@example.com");
        EventDto<CustomerRegistered> originalEventDto =
                EventDto.from(event.customerId().id(),
                              event.eventSequence(),
                              1L,
                              event);

        String csv = CsvStringsEventStore.toCsv(originalEventDto);
        assertThat(csv)
                .isEqualTo("3ed83b5e-9564-42a5-8329-3cc5d5e14840,0,1,dev.ted.jitterticket.eventsourced.domain.customer.CustomerRegistered,{\"customerId\":{\"id\":\"3ed83b5e-9564-42a5-8329-3cc5d5e14840\"},\"eventSequence\":0,\"customerName\":\"Customer Name\",\"email\":\"customer@example.com\"}");

        CsvStringsEventStore<ConcertId, ConcertEvent, Concert> concertEventStore = (CsvStringsEventStore<ConcertId, ConcertEvent, Concert>) CsvStringsEventStore.forConcerts(new ArrayListStringsReaderAppender());
        EventDto<? extends Event> eventDto = concertEventStore.csvToEventDto(csv);
        assertThat(eventDto)
                .isEqualTo(originalEventDto);

        Event eventFromDto = eventDto.toDomain();
        assertThat(eventFromDto)
                .isEqualTo(event);
    }

}