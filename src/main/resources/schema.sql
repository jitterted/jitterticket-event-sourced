-- noinspection GrazieInspectionForFile

-- Events table for storing event-sourced aggregates
CREATE TABLE IF NOT EXISTS events (
    aggregate_root_id UUID NOT NULL,
    -- This is a global sequence for ordering events (within and across aggregates)
    -- per Postgres docs, this starts at 1
    event_sequence BIGSERIAL,
    event_type TEXT NOT NULL,
    json TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),

    -- Primary key is composite of aggregate_root_id and event_sequence
    PRIMARY KEY (aggregate_root_id, event_sequence)

    -- Optional: Add a version field for schema versioning
    , version INTEGER DEFAULT 1
);

-- Index for global ordering, e.g., for queries like findAllAfter(event_sequence)
CREATE INDEX IF NOT EXISTS idx_events_event_sequence ON events (event_sequence);

-- Index for timestamp-based queries
CREATE INDEX IF NOT EXISTS idx_events_created_at ON events (created_at);


-- == PROJECTIONS == --

-- Table for storing just the last event sequence that a Projection has seen (even if its projection data didn't change: it still "saw" it)
CREATE TABLE IF NOT EXISTS projection_metadata (
    projection_name TEXT NOT NULL PRIMARY KEY,
    last_event_sequence_seen BIGINT DEFAULT 0
);


-- Table for storing the concert sales summary projectionMetadata
CREATE TABLE IF NOT EXISTS concert_sales_projection (
    concert_id   UUID PRIMARY KEY,
    artist_name  TEXT    NOT NULL,
    concert_date DATE    NOT NULL, -- Maps directly to Java LocalDate
    tickets_sold INTEGER NOT NULL DEFAULT 0,
    total_sales  INTEGER NOT NULL DEFAULT 0
);
