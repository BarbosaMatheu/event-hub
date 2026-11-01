-- Event model v2: clearer column names, ticket accounting and DB-level invariants.
-- NOTE: V1-V5 are never edited once applied; schema changes are always new migrations.

ALTER TABLE TB_EVENTS RENAME COLUMN local TO location;
ALTER TABLE TB_EVENTS RENAME COLUMN date_time TO starts_at;
ALTER TABLE TB_EVENTS RENAME COLUMN maximum_capacity TO capacity;

-- Clean legacy rows so the NOT NULL / CHECK constraints below can be applied safely.
UPDATE TB_EVENTS SET location = 'TBD' WHERE location IS NULL;
UPDATE TB_EVENTS SET starts_at = now() WHERE starts_at IS NULL;
UPDATE TB_EVENTS SET capacity = 1 WHERE capacity IS NULL OR capacity < 1;
UPDATE TB_EVENTS SET price = 0 WHERE price IS NULL OR price < 0;

ALTER TABLE TB_EVENTS ALTER COLUMN description TYPE VARCHAR(2000);
ALTER TABLE TB_EVENTS ALTER COLUMN location SET NOT NULL;
ALTER TABLE TB_EVENTS ALTER COLUMN starts_at SET NOT NULL;
ALTER TABLE TB_EVENTS ALTER COLUMN capacity SET NOT NULL;
ALTER TABLE TB_EVENTS ALTER COLUMN price SET NOT NULL;

ALTER TABLE TB_EVENTS ADD COLUMN tickets_sold INTEGER NOT NULL DEFAULT 0;
ALTER TABLE TB_EVENTS ADD COLUMN image_url VARCHAR(500);
ALTER TABLE TB_EVENTS ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE TB_EVENTS ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT now();

-- Defense in depth: even a bug in the application can never oversell an event.
ALTER TABLE TB_EVENTS ADD CONSTRAINT ck_events_capacity CHECK (capacity > 0);
ALTER TABLE TB_EVENTS ADD CONSTRAINT ck_events_price CHECK (price >= 0);
ALTER TABLE TB_EVENTS ADD CONSTRAINT ck_events_tickets_sold CHECK (tickets_sold >= 0 AND tickets_sold <= capacity);
