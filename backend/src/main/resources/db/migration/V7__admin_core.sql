-- Admin core: tenants/branches active flags, branch settings, nullable staff branch for super admin

ALTER TABLE tenants
  ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE branches
  ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE staff_users
  ALTER COLUMN branch_id DROP NOT NULL;

CREATE TABLE IF NOT EXISTS branch_settings (
  branch_id BIGINT PRIMARY KEY REFERENCES branches(id) ON DELETE CASCADE,
  require_otp_for_first_order BOOLEAN NULL,
  otp_ttl_seconds INT NULL,
  otp_max_attempts INT NULL,
  otp_resend_cooldown_seconds INT NULL,
  otp_length INT NULL,
  otp_dev_echo_code BOOLEAN NULL,
  enable_party_pin BOOLEAN NULL,
  allow_pay_other_guests_items BOOLEAN NULL,
  allow_pay_whole_table BOOLEAN NULL,
  tips_enabled BOOLEAN NULL,
  tips_percentages TEXT NULL
);
