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

CREATE TABLE IF NOT EXISTS concert_sales_projection
(
    projection_name          TEXT NOT NULL PRIMARY KEY,
    last_event_sequence_seen BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS concert_sales
(
    concert_id               UUID PRIMARY KEY,
    artist_name              TEXT NOT NULL ,
    concert_date             DATE NOT NULL ,
    tickets_sold             INTEGER NOT NULL,
    total_sales              INTEGER NOT NULL,

    -- Foreign key to the parent
    concert_sales_projection TEXT NOT NULL,
    CONSTRAINT fk_projection FOREIGN KEY (concert_sales_projection)
        REFERENCES concert_sales_projection (projection_name)
            ON DELETE CASCADE
);
