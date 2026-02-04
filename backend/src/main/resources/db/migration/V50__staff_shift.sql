ALTER TABLE staff_users
  ADD COLUMN IF NOT EXISTS shift_started_at timestamptz;
