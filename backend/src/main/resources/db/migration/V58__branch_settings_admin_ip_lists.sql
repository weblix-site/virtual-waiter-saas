ALTER TABLE branch_settings
  ADD COLUMN IF NOT EXISTS admin_ip_allowlist TEXT;

ALTER TABLE branch_settings
  ADD COLUMN IF NOT EXISTS admin_ip_denylist TEXT;
