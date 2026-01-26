-- Bill requests (offline payment) + close order items

CREATE TABLE IF NOT EXISTS bill_requests (
  id BIGSERIAL PRIMARY KEY,
  table_id BIGINT NOT NULL REFERENCES tables(id),
  guest_session_id BIGINT NOT NULL REFERENCES guest_sessions(id),
  party_id BIGINT NULL REFERENCES table_parties(id),
  mode TEXT NOT NULL, -- MY | SELECTED | WHOLE_TABLE
  payment_method TEXT NOT NULL, -- CASH | TERMINAL
  status TEXT NOT NULL DEFAULT 'CREATED', -- CREATED | PAID_CONFIRMED | CANCELLED
  subtotal_cents INT NOT NULL,
  tips_percent INT NULL,
  tips_amount_cents INT NOT NULL DEFAULT 0,
  total_cents INT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  confirmed_at TIMESTAMPTZ NULL,
  confirmed_by_staff_id BIGINT NULL REFERENCES staff_users(id)
);

CREATE TABLE IF NOT EXISTS bill_request_items (
  id BIGSERIAL PRIMARY KEY,
  bill_request_id BIGINT NOT NULL REFERENCES bill_requests(id) ON DELETE CASCADE,
  order_item_id BIGINT NOT NULL REFERENCES order_items(id),
  line_total_cents INT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE order_items
  ADD COLUMN IF NOT EXISTS bill_request_id BIGINT NULL REFERENCES bill_requests(id);

ALTER TABLE order_items
  ADD COLUMN IF NOT EXISTS is_closed BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE order_items
  ADD COLUMN IF NOT EXISTS closed_at TIMESTAMPTZ NULL;

CREATE INDEX IF NOT EXISTS idx_bill_requests_status_created_at ON bill_requests(status, created_at);
CREATE INDEX IF NOT EXISTS idx_bill_request_items_bill_request_id ON bill_request_items(bill_request_id);
