-- Audit User-Agent for guest actions

ALTER TABLE orders
  ADD COLUMN IF NOT EXISTS created_by_ua TEXT NULL;

ALTER TABLE waiter_calls
  ADD COLUMN IF NOT EXISTS created_by_ua TEXT NULL;

ALTER TABLE bill_requests
  ADD COLUMN IF NOT EXISTS created_by_ua TEXT NULL;
