-- MVP extensions: staff users, waiter calls, richer menu items, order item comments/modifiers

CREATE TABLE IF NOT EXISTS staff_users (
  id BIGSERIAL PRIMARY KEY,
  branch_id BIGINT NOT NULL REFERENCES branches(id),
  username TEXT NOT NULL UNIQUE,
  password_hash TEXT NOT NULL,
  role TEXT NOT NULL,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS waiter_calls (
  id BIGSERIAL PRIMARY KEY,
  table_id BIGINT NOT NULL REFERENCES tables(id),
  guest_session_id BIGINT REFERENCES guest_sessions(id),
  status TEXT NOT NULL DEFAULT 'NEW',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Extra fields for menu item “характеристики”
ALTER TABLE menu_items
  ADD COLUMN IF NOT EXISTS ingredients_ru TEXT,
  ADD COLUMN IF NOT EXISTS ingredients_ro TEXT,
  ADD COLUMN IF NOT EXISTS ingredients_en TEXT,
  ADD COLUMN IF NOT EXISTS allergens TEXT,
  ADD COLUMN IF NOT EXISTS weight TEXT,
  ADD COLUMN IF NOT EXISTS tags TEXT,
  ADD COLUMN IF NOT EXISTS photo_urls TEXT;

-- Order item comments/modifiers
ALTER TABLE order_items
  ADD COLUMN IF NOT EXISTS comment TEXT,
  ADD COLUMN IF NOT EXISTS modifiers_json TEXT;
