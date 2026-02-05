ALTER TABLE staff_users
  ADD COLUMN IF NOT EXISTS permissions TEXT;
