-- noinspection GrazieInspectionForFile

-- Events table for storing event-sourced aggregates
CREATE TABLE IF NOT EXISTS events (
    aggregate_root_id UUID NOT NULL,
    event_sequence INTEGER NOT NULL,
    event_type TEXT NOT NULL,
    json TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),

    -- Primary key is composite of aggregate_root_id and event_sequence
    PRIMARY KEY (aggregate_root_id, event_sequence)
    
    -- Add a global sequence for ordering all events across aggregates
    -- as per docs, this starts at 1
    , global_sequence BIGSERIAL
    
    -- Optional: Add a version field for schema versioning
    , version INTEGER DEFAULT 1
);

-- Index for global ordering if using global_sequence
CREATE INDEX IF NOT EXISTS idx_events_global_sequence ON events (global_sequence);

-- Index for timestamp-based queries
CREATE INDEX IF NOT EXISTS idx_events_created_at ON events (created_at);


-- == PROJECTIONS == --

-- Table for storing just the last global event sequence that a Projection has seen/processed
CREATE TABLE IF NOT EXISTS projections (
    projection_name TEXT NOT NULL PRIMARY KEY,
    version INTEGER DEFAULT 1,
    last_global_event_sequence_seen BIGINT DEFAULT 0
);


-- Table for storing the concert sales summary projection
CREATE TABLE IF NOT EXISTS concert_sales_projection (
    concert_id   UUID PRIMARY KEY,
    artist_name  TEXT    NOT NULL,
    concert_date DATE    NOT NULL, -- Maps directly to Java LocalDate
    tickets_sold INTEGER NOT NULL DEFAULT 0,
    total_sales  INTEGER NOT NULL DEFAULT 0
);
