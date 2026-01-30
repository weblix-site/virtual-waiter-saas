-- Modifiers: groups, options, menu item bindings + order snapshot columns

CREATE TABLE IF NOT EXISTS modifier_groups (
  id BIGSERIAL PRIMARY KEY,
  branch_id BIGINT NOT NULL REFERENCES branches(id),
  name_ru TEXT NOT NULL,
  name_ro TEXT,
  name_en TEXT,
  is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS modifier_options (
  id BIGSERIAL PRIMARY KEY,
  group_id BIGINT NOT NULL REFERENCES modifier_groups(id) ON DELETE CASCADE,
  name_ru TEXT NOT NULL,
  name_ro TEXT,
  name_en TEXT,
  price_cents INT NOT NULL DEFAULT 0,
  is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS menu_item_modifier_groups (
  id BIGSERIAL PRIMARY KEY,
  menu_item_id BIGINT NOT NULL REFERENCES menu_items(id) ON DELETE CASCADE,
  group_id BIGINT NOT NULL REFERENCES modifier_groups(id) ON DELETE CASCADE,
  is_required BOOLEAN NOT NULL DEFAULT FALSE,
  min_select INT NULL,
  max_select INT NULL,
  sort_order INT NOT NULL DEFAULT 0
);

ALTER TABLE order_items
  ADD COLUMN IF NOT EXISTS base_price_cents INT NULL;

ALTER TABLE order_items
  ADD COLUMN IF NOT EXISTS modifiers_price_cents INT NULL;
