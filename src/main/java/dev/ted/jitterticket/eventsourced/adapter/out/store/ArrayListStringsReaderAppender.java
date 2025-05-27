package dev.ted.jitterticket.eventsourced.adapter.out.store;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class ArrayListStringsReaderAppender implements StringsReaderAppender {
    private final List<String> csvLines = new ArrayList<>();

    @Override
    public void appendLines(List<String> newCsvLines) {
        csvLines.addAll(newCsvLines);
    }

    @Override
    public Stream<String> readAllLines() {
        return csvLines.stream();
    }
}