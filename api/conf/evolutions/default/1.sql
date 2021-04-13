-- organizations_event_store schema

-- !Ups
CREATE TABLE organizations_event_store
(
    global_offset       BIGSERIAL     NOT NULL,
    aggregate_root_id   BIGINT NOT NULL,
    aggregate_version   INTEGER  NOT NULL,
    shard               INTEGER  NOT NULL,
    event_type          TEXT NOT NULL,
    event               JSONB NOT NULL,
    time                TIMESTAMP NOT NULL,
    action_performed_by BIGINT,
    ip                  TEXT,
    other_context_data  JSONB,
    PRIMARY KEY (global_offset),
    UNIQUE (aggregate_root_id, aggregate_version)
);

CREATE INDEX ON organizations_event_store (global_offset, shard);

