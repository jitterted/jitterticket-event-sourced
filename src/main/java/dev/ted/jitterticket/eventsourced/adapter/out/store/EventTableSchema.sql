-- Events table for storing event-sourced aggregates
CREATE TABLE IF NOT EXISTS events (
    aggregate_root_id UUID NOT NULL,
    event_sequence INTEGER NOT NULL,
    event_type TEXT NOT NULL,
    json_content JSONB NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),

    -- Primary key is composite of aggregate_root_id and event_sequence
    PRIMARY KEY (aggregate_root_id, event_sequence),
    
    -- Optional: Add a global sequence for ordering all events across aggregates
    global_sequence BIGSERIAL,
    
    -- Optional: Add version field for schema versioning
    version INTEGER DEFAULT 1
);

-- Index for global ordering if using global_sequence
-- CREATE INDEX IF NOT EXISTS idx_events_global_sequence ON events (global_sequence);

-- Index for timestamp-based queries
-- CREATE INDEX IF NOT EXISTS idx_events_created_at ON events (created_at);
