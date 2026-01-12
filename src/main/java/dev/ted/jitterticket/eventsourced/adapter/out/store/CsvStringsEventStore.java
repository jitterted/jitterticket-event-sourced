package dev.ted.jitterticket.eventsourced.adapter.out.store;

import dev.ted.jitterticket.eventsourced.application.BaseEventStore;
import dev.ted.jitterticket.eventsourced.application.Checkpoint;
import dev.ted.jitterticket.eventsourced.application.port.EventStore;
import dev.ted.jitterticket.eventsourced.domain.Event;
import dev.ted.jitterticket.eventsourced.domain.EventSourcedAggregate;
import dev.ted.jitterticket.eventsourced.domain.Id;
import dev.ted.jitterticket.eventsourced.domain.concert.Concert;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import dev.ted.jitterticket.eventsourced.domain.customer.Customer;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerEvent;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerId;
import jakarta.annotation.Nonnull;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Represents a CSV-based implementation of an event store, which is responsible
 * for persisting and retrieving domain events related to event-sourced aggregates.
 *
 * @param <ID>        The type of the aggregate identifier, which must extend {@code Id}.
 * @param <EVENT>     The type of the domain event, which must extend {@code Event}.
 * @param <AGGREGATE> The type of the event-sourced aggregate, which must extend {@code EventSourcedAggregate}.
 */
public class CsvStringsEventStore<ID extends Id, EVENT extends Event, AGGREGATE extends EventSourcedAggregate<EVENT, ID>> extends BaseEventStore<ID, EVENT, AGGREGATE> {

    private final StringsReaderAppender stringsReaderAppender;

    private CsvStringsEventStore(Function<List<EVENT>, AGGREGATE> eventsToAggregate,
                                 StringsReaderAppender stringsReaderAppender) {
        super(eventsToAggregate);
        this.stringsReaderAppender = stringsReaderAppender;
    }

    public static EventStore<ConcertId, ConcertEvent, Concert> forConcerts(StringsReaderAppender stringsReaderAppender) {
        return new CsvStringsEventStore<>(Concert::reconstitute,
                                          stringsReaderAppender);
    }

    public static EventStore<CustomerId, CustomerEvent, Customer> forCustomers(StringsReaderAppender stringsReaderAppender) {
        return new CsvStringsEventStore<>(Customer::reconstitute,
                                          stringsReaderAppender);
    }

    static String toCsv(EventDto<? extends Event> originalEventDto) {
        return originalEventDto.getAggregateRootId() + ","
               + originalEventDto.getEventSequence() + ","
               + originalEventDto.getEventType() + ","
               + originalEventDto.getJson();
    }

    EventDto<EVENT> csvToEventDto(String csv) {
        String[] splitCsv = csv.split(",", 4);
        UUID aggregateRootId = UUID.fromString(splitCsv[0]);
        long eventSequence = Long.parseLong(splitCsv[1]);
        String eventType = splitCsv[2];
        String json = splitCsv[3];

        return new EventDto<>(aggregateRootId,
                              eventSequence,
                              eventType,
                              json);
    }

    @Override
    public Stream<EVENT> save(ID aggregateId, Stream<EVENT> uncommittedEvents) {
        AtomicLong eventSequence = new AtomicLong(
                stringsReaderAppender.readAllLines().count());
        List<EventDto<EVENT>> uncommittedEventDtos =
                uncommittedEvents.map(event -> EventDto.from(
                                         aggregateId.id(),
                                         eventSequence.incrementAndGet(),
                                         event))
                                 .toList();
        List<String> newCsvLines = uncommittedEventDtos
                .stream()
                .map(CsvStringsEventStore::toCsv)
                .toList();
        stringsReaderAppender.appendLines(newCsvLines);
        return uncommittedEventDtos.stream()
                                   .map(EventDto::toDomain);
    }

    @Override
    protected @Nonnull List<EventDto<EVENT>> eventDtosFor(ID id) {
        return allEventDtos()
                .filter(dto -> dto.getAggregateRootId().equals(id.id()))
                .sorted(Comparator.comparingLong(EventDto::getEventSequence))
                .toList();
    }

    @Override
    public Stream<EVENT> allEventsAfter(Checkpoint checkpoint) {
        return allEventDtos()
                .dropWhile(eventDto -> eventDto.getEventSequence() <= checkpoint.value())
                .map(EventDto::toDomain);

    }

    /**
     * Reads all the lines from the CSV
     * <p>
     * by definition, these will be in the correct global order
     * because we are always appending to the "file"
     *
     */
    private Stream<EventDto<EVENT>> allEventDtos() {
        return stringsReaderAppender.readAllLines()
                                    .map(this::csvToEventDto);
    }
}
