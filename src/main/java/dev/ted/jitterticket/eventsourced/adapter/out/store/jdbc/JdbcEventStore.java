package dev.ted.jitterticket.eventsourced.adapter.out.store.jdbc;

import dev.ted.jitterticket.Gatherers;
import dev.ted.jitterticket.eventsourced.adapter.out.store.EventDto;
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
    private final Class<EVENT> concreteEventClass;

    private JdbcEventStore(Function<List<EVENT>, AGGREGATE> eventsToAggregate,
                           EventDboRepository eventDboRepository,
                           Class<EVENT> concreteEventClass) {
        super(eventsToAggregate);
        this.eventDboRepository = eventDboRepository;
        this.concreteEventClass = concreteEventClass;
    }


    public static EventStore<ConcertId, ConcertEvent, Concert> forConcerts(EventDboRepository eventDboRepository) {
        return new JdbcEventStore<>(Concert::reconstitute, eventDboRepository, ConcertEvent.class);
    }

    public static EventStore<CustomerId, CustomerEvent, Customer> forCustomers(EventDboRepository eventDboRepository) {
        return new JdbcEventStore<>(Customer::reconstitute, eventDboRepository, CustomerEvent.class);
    }


    @Override
    protected List<EventDto<EVENT>> eventDtosFor(ID id) {
        return eventDboRepository.findByAggregateRootId(id.id())
                                 .stream()
                                 .map(dbo -> new EventDto<EVENT>(
                                         dbo.getAggregateRootId(),
                                         dbo.getEventSequence(),
                                         null, dbo.getEventType(),
                                         dbo.getJson()))
                                 .toList();
    }

    @Override
    public void save(ID aggregateId, Stream<EVENT> uncommittedEvents) {
        // assumes there's at least one uncommittedEvent in the incoming stream!
        List<EventDbo> dbos = uncommittedEvents
                .map(event -> EventDto.from(aggregateId.id(), event.eventSequence(), null, event))
                .map(dto -> new EventDbo(
                        dto.getAggregateRootId(),
                        dto.getEventSequence(),
                        dto.getEventType(),
                        dto.getJson()))
                .toList();
        Iterable<EventDbo> savedEventDbos = eventDboRepository.saveAll(dbos);
//        long highestGlobalSequence = StreamSupport.stream(savedEventDbos.spliterator(), false)
//                                .mapToLong(EventDbo::getGlobalSequence)
//                                .max()
//                                .orElseThrow();
//        return highestGlobalSequence;
    }

    @Override
    public Stream<EVENT> allEvents() {
        return eventDboRepository.findAllByGlobalSequence()
                                 .stream()
                                 .map(dbo -> new EventDto<EVENT>(
                                         dbo.getAggregateRootId(),
                                         dbo.getEventSequence(),
                                         null, dbo.getEventType(),
                                         dbo.getJson()))
                                 .map(EventDto::toDomain)
                                 .gather(Gatherers.filterAndCastTo(concreteEventClass));
    }

    @Override
    public Stream<EVENT> allEventsAfter(long globalEventSequence) {
        return Stream.empty();
    }
}
