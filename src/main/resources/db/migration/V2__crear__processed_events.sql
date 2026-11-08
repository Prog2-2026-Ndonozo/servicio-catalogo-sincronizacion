CREATE TABLE processed_events (
                                  event_id     TEXT        PRIMARY KEY CHECK (btrim(event_id) <> ''),
                                  new_version  BIGINT      NOT NULL CHECK (new_version > 0),
                                  processed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);