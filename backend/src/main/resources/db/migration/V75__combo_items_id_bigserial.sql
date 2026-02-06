-- Ensure combo_items.id is truly BIGINT and sequence matches
ALTER TABLE combo_items
  ALTER COLUMN id TYPE BIGINT USING id::bigint;

ALTER SEQUENCE IF EXISTS combo_items_id_seq AS BIGINT;
