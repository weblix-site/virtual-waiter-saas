-- Guest session security fields for anti-QR-photo protection

ALTER TABLE guest_sessions
  ADD COLUMN IF NOT EXISTS session_secret TEXT NULL,
  ADD COLUMN IF NOT EXISTS created_by_ip TEXT NULL,
  ADD COLUMN IF NOT EXISTS created_by_ua TEXT NULL;

CREATE INDEX IF NOT EXISTS idx_guest_sessions_secret ON guest_sessions(session_secret);
