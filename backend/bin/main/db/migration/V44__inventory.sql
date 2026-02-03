-- Inventory / stock (MVP)

ALTER TABLE branch_settings
  ADD COLUMN IF NOT EXISTS inventory_enabled boolean;

CREATE TABLE IF NOT EXISTS inventory_items (
  id bigserial PRIMARY KEY,
  branch_id bigint NOT NULL,
  name_ru varchar(200) NOT NULL,
  name_ro varchar(200),
  name_en varchar(200),
  unit varchar(16) NOT NULL DEFAULT 'pcs',
  qty_on_hand double precision NOT NULL DEFAULT 0,
  min_qty double precision NOT NULL DEFAULT 0,
  is_active boolean NOT NULL DEFAULT true,
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  updated_at timestamp with time zone
);
CREATE INDEX IF NOT EXISTS idx_inventory_items_branch ON inventory_items(branch_id);

CREATE TABLE IF NOT EXISTS menu_item_ingredients (
  id bigserial PRIMARY KEY,
  menu_item_id bigint NOT NULL,
  inventory_item_id bigint NOT NULL,
  qty_per_item double precision NOT NULL,
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  updated_at timestamp with time zone,
  UNIQUE(menu_item_id, inventory_item_id)
);
CREATE INDEX IF NOT EXISTS idx_menu_item_ingredients_item ON menu_item_ingredients(menu_item_id);
