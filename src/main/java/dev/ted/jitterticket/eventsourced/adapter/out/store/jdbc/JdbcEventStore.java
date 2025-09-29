package dev.ted.jitterticket.eventsourced.adapter.out.store.jdbc;

import dev.ted.jitterticket.eventsourced.adapter.out.store.EventDto;
import dev.ted.jitterticket.eventsourced.adapter.out.store.StringsReaderAppender;
import dev.ted.jitterticket.eventsourced.application.BaseEventStore;
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

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

public class JdbcEventStore<ID extends Id, EVENT extends Event, AGGREGATE extends EventSourcedAggregate<EVENT, ID>> extends BaseEventStore<ID, EVENT, AGGREGATE> {

    private final EventDboRepository eventDboRepository;

    public JdbcEventStore(Function<List<EVENT>, AGGREGATE> eventsToAggregate,
                          EventDboRepository eventDboRepository) {
        super(eventsToAggregate);
        this.eventDboRepository = eventDboRepository;
    }



    public static EventStore<ConcertId, ConcertEvent, Concert> forConcerts(StringsReaderAppender stringsReaderAppender, EventDboRepository eventDboRepository) {
        return new JdbcEventStore<>(Concert::reconstitute,
                eventDboRepository);
    }

    public static EventStore<CustomerId, CustomerEvent, Customer> forCustomers(StringsReaderAppender stringsReaderAppender, EventDboRepository eventDboRepository) {
        return new JdbcEventStore<>(Customer::reconstitute,
                eventDboRepository);
    }


    @Override
    protected List<EventDto<EVENT>> eventDtosFor(ID id) {
        return eventDboRepository.findByAggregateRootId(id.id())
                                 .stream()
                                 .map(dbo -> new EventDto<EVENT>(
                                         dbo.getAggregateRootId(),
                                         dbo.getEventSequence(),
                                         dbo.getEventType(),
                                         dbo.getJson()))
                                 .toList();
    }

    @Override
    public void save(ID aggregateId, Stream<EVENT> uncommittedEvents) {
        List<EventDbo> dbos = uncommittedEvents
                .map(event -> EventDto.from(aggregateId.id(), event.eventSequence(), event))
                .map(dto -> new EventDbo(dto.getAggregateRootId(), dto.getEventSequence(), dto.getEventType(), dto.getJson()))
                .toList();
        eventDboRepository.saveAll(dbos);
    }

    @Override
    public Stream<EVENT> allEvents() {
        return eventDboRepository.findAllByGlobalSequence()
                                 .stream()
                                 .map(dbo -> new EventDto<EVENT>(
                                         dbo.getAggregateRootId(),
                                         dbo.getEventSequence(),
                                         dbo.getEventType(),
                                         dbo.getJson()))
                                 .map(EventDto::toDomain);
    }
}
