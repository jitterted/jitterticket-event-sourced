package dev.ted.jitterticket;

import java.util.stream.Gatherer;

public class Gatherers {
    public static <EVENT, TARGET extends EVENT> Gatherer<EVENT, Void, TARGET> filterAndCastTo(Class<TARGET> eventClass) {
        return Gatherer.of(
                (Void _, EVENT event, Gatherer.Downstream<? super TARGET> downstream) -> {
                    if (eventClass.isInstance(event)) {
                        downstream.push(eventClass.cast(event));
                    }
                    return !downstream.isRejecting();
                }
        );
    }

    public static <EVENT, TARGET extends EVENT> Gatherer<EVENT, Void, TARGET> castTo(Class<TARGET> eventClass) {
        return Gatherer.of(
                (Void _, EVENT event, Gatherer.Downstream<? super TARGET> downstream) -> {
                    downstream.push(eventClass.cast(event));
                    return !downstream.isRejecting();
                }
        );
    }
}
