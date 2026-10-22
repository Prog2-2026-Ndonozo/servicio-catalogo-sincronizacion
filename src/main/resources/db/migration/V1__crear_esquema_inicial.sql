CREATE TABLE professional_categories (
                                         id          BIGINT      PRIMARY KEY,
                                         name        TEXT        NOT NULL CHECK (btrim(name) <> ''),
                                         description TEXT        NOT NULL,
                                         enabled     BOOLEAN     NOT NULL,
                                         created_at  TIMESTAMPTZ NOT NULL,
                                         updated_at  TIMESTAMPTZ NOT NULL
);

CREATE TABLE professionals (
                               id          BIGINT      PRIMARY KEY,
                               category_id BIGINT      NOT NULL
                                   REFERENCES professional_categories (id) DEFERRABLE INITIALLY DEFERRED,
                               first_name  TEXT        NOT NULL CHECK (btrim(first_name) <> ''),
                               last_name   TEXT        NOT NULL CHECK (btrim(last_name) <> ''),
                               enabled     BOOLEAN     NOT NULL,
                               created_at  TIMESTAMPTZ NOT NULL,
                               updated_at  TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_professionals_category_id ON professionals (category_id);

CREATE TABLE weekly_schedules (
                                  id                    BIGINT      PRIMARY KEY,
                                  professional_id       BIGINT      NOT NULL
                                      REFERENCES professionals (id) DEFERRABLE INITIALLY DEFERRED,
                                  day_of_week           TEXT        NOT NULL CHECK (day_of_week IN
                                                                                    ('MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY')),
                                  start_time            TIME        NOT NULL,
                                  end_time              TIME        NOT NULL,
                                  slot_duration_minutes INTEGER     NOT NULL CHECK (slot_duration_minutes > 0),
                                  enabled               BOOLEAN     NOT NULL,
                                  created_at            TIMESTAMPTZ NOT NULL,
                                  updated_at            TIMESTAMPTZ NOT NULL,
                                  CHECK (start_time < end_time)
);

CREATE INDEX idx_weekly_schedules_professional_id ON weekly_schedules (professional_id);

CREATE TABLE catalog_version (
                                 id               SMALLINT PRIMARY KEY CHECK (id = 1),
                                 snapshot_version BIGINT   NOT NULL CHECK (snapshot_version >= 0)
);

INSERT INTO catalog_version (id, snapshot_version) VALUES (1, 0);