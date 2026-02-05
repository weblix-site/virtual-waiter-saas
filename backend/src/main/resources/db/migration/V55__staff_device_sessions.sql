ALTER TABLE staff_device_tokens
  ADD COLUMN IF NOT EXISTS device_id TEXT;

ALTER TABLE staff_device_tokens
  ADD COLUMN IF NOT EXISTS device_name TEXT;

ALTER TABLE staff_device_tokens
  ADD COLUMN IF NOT EXISTS revoked_at TIMESTAMPTZ;
