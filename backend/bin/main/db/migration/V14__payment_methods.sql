-- Payment methods flags per branch

ALTER TABLE branch_settings
  ADD COLUMN IF NOT EXISTS pay_cash_enabled BOOLEAN NULL,
  ADD COLUMN IF NOT EXISTS pay_terminal_enabled BOOLEAN NULL;
