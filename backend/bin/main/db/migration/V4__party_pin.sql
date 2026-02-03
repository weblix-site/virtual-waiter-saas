-- Party PIN feature
CREATE TABLE IF NOT EXISTS table_parties (
  id BIGSERIAL PRIMARY KEY,
  table_id BIGINT NOT NULL REFERENCES tables(id),
  pin CHAR(4) NOT NULL,
  status TEXT NOT NULL DEFAULT 'ACTIVE',
  expires_at TIMESTAMPTZ NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_table_parties_table_pin ON table_parties(table_id, pin);
CREATE INDEX IF NOT EXISTS idx_table_parties_expires ON table_parties(expires_at);

ALTER TABLE guest_sessions
  ADD COLUMN IF NOT EXISTS party_id BIGINT NULL REFERENCES table_parties(id);
