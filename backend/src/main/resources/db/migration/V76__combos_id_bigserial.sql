-- Ensure combos.id is BIGINT and sequence matches
ALTER TABLE combos
  ALTER COLUMN id TYPE BIGINT USING id::bigint;

ALTER SEQUENCE IF EXISTS combos_id_seq AS BIGINT;
