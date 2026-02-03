-- Track which staff handled an order (for waiter stats)

ALTER TABLE orders
  ADD COLUMN IF NOT EXISTS handled_by_staff_id BIGINT NULL REFERENCES staff_users(id);

CREATE INDEX IF NOT EXISTS idx_orders_handled_by_staff ON orders(handled_by_staff_id);
