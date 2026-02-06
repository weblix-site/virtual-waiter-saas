CREATE TABLE IF NOT EXISTS combos (
  id SERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  branch_id BIGINT NOT NULL,
  menu_item_id BIGINT NOT NULL,
  is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS idx_combos_branch_id ON combos(branch_id);
CREATE INDEX IF NOT EXISTS idx_combos_tenant_id ON combos(tenant_id);

CREATE TABLE IF NOT EXISTS combo_items (
  id SERIAL PRIMARY KEY,
  combo_id BIGINT NOT NULL,
  menu_item_id BIGINT NOT NULL,
  min_qty INT NOT NULL DEFAULT 0,
  max_qty INT NOT NULL DEFAULT 1,
  sort_order INT NOT NULL DEFAULT 0,
  is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS idx_combo_items_combo_id ON combo_items(combo_id);
