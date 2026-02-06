ALTER TABLE branch_settings
  ADD COLUMN IF NOT EXISTS time_zone TEXT;
