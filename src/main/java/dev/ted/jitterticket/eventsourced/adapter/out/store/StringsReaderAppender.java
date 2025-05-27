package dev.ted.jitterticket.eventsourced.adapter.out.store;

import java.util.List;
import java.util.stream.Stream;

public interface StringsReaderAppender {
    void appendLines(List<String> newCsvLines);

    Stream<String> readAllLines();
}
