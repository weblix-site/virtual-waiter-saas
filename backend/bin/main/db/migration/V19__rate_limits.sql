-- Rate limit timestamps per guest session

ALTER TABLE guest_sessions
  ADD COLUMN IF NOT EXISTS last_order_at TIMESTAMPTZ NULL,
  ADD COLUMN IF NOT EXISTS last_waiter_call_at TIMESTAMPTZ NULL,
  ADD COLUMN IF NOT EXISTS last_bill_request_at TIMESTAMPTZ NULL;
