-- Party close

ALTER TABLE table_parties
  ADD COLUMN IF NOT EXISTS closed_at TIMESTAMPTZ NULL;
