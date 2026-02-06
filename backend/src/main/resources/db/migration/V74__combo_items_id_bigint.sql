-- Align combo_items.id type with JPA (BIGINT)
ALTER TABLE combo_items
  ALTER COLUMN id TYPE BIGINT;
