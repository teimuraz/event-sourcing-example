-- kafka_connectors schema

-- !Ups

CREATE TABLE kafka_connectors
(
    connector_id TEXT NOT NULL,
    shard        INTEGER NOT NULL,
    last_offset  BIGINT NOT NULL,
    PRIMARY KEY (connector_id, shard, last_offset)
);

