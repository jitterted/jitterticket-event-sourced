package dev.ted.jitterticket.eventsourced.adapter.out.store;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class CsvReaderAppenderTest {

    @Test
    void fileCreatedIfDoesNotExist(@TempDir Path tempDir) throws IOException {
        Path tempFile = tempDir.resolve("test.csv");
        assertThat(Files.exists(tempFile))
                .as("File should not yet exist")
                .isFalse();
        
        CsvReaderAppender csvReaderAppender = new CsvReaderAppender(tempFile);

        assertThat(Files.exists(tempFile))
                .as("Expected CsvReaderAppender to create non-existent file")
                .isTrue();
    }

    @Test
    void writtenAndAppendedLinesCanBeReadFromFile(@TempDir Path tempDir) throws IOException {
        Path tempFile = tempDir.resolve("test.csv");

        CsvReaderAppender csvReaderAppender = new CsvReaderAppender(tempFile);

        csvReaderAppender.appendLines(List.of(
                "Line 1",
                "Line 2"
        ));
        csvReaderAppender.appendLines(List.of(
                "Line 3",
                "Line 4"
        ));

        assertThat(csvReaderAppender.readAllLines())
                .as("Expected all lines to appear in file as each write should append, not overwrite")
                .containsExactly("Line 1", "Line 2", "Line 3", "Line 4");
    }
}