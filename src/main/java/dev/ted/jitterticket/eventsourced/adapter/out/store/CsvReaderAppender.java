package dev.ted.jitterticket.eventsourced.adapter.out.store;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Handles all read/append I/O to a file
 * Default filename = "events.csv"
 * <p>
 * all strings (typically events as CSV) are written (appended) to the single file
 * <p>
 * Pros: easy to write, especially when writing multiple events that are for different aggregates
 * (e.g., a ticket purchase that generates an event for the Concert and for the Customer)
 * Cons: might cause contention for multiple simultaneous writes
 */
public class CsvReaderAppender implements StringsReaderAppender {
    private final Path filePath;
    
    /**
     * Creates a CsvReaderAppender with the specified file path.
     * Ensures the file and parent directories exist upon creation.
     * 
     * @param filePath The path to the events file
     */
    public CsvReaderAppender(Path filePath) throws IOException {
        Objects.requireNonNull(filePath);
        this.filePath = filePath;
        ensureFileExists();
    }
    
    /**
     * Creates a CsvReaderAppender with the default "events.csv" file path.
     * Ensures the file and parent directories exist upon creation.
     */
    public CsvReaderAppender() throws IOException {
        this(Path.of("events.csv"));
    }
    
    private void ensureFileExists() throws IOException {
        // getParent will return null if the filePath is a plain filename with no directories mentioned
        Path parent = filePath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        if (!Files.exists(filePath)) {
            Files.createFile(filePath);
        }
    }
    
    public Stream<String> readAllLines()  {
        try {
            return Files.lines(filePath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public void appendLines(List<String> lines) {
        try {
            Files.write(
                filePath,
                lines,
                StandardCharsets.UTF_8,
                StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}