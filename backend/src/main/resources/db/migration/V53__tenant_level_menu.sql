-- Move menu categories and modifier groups to tenant scope, add branch overrides for menu items.

ALTER TABLE menu_categories ADD COLUMN tenant_id BIGINT;
UPDATE menu_categories mc
SET tenant_id = b.tenant_id
FROM branches b
WHERE mc.branch_id = b.id;
ALTER TABLE menu_categories ALTER COLUMN tenant_id SET NOT NULL;

ALTER TABLE modifier_groups ADD COLUMN tenant_id BIGINT;
UPDATE modifier_groups mg
SET tenant_id = b.tenant_id
FROM branches b
WHERE mg.branch_id = b.id;
ALTER TABLE modifier_groups ALTER COLUMN tenant_id SET NOT NULL;

-- Remove branch_id from tenant-level dictionaries.
ALTER TABLE menu_categories DROP COLUMN branch_id;
ALTER TABLE modifier_groups DROP COLUMN branch_id;

CREATE TABLE branch_menu_item_overrides (
  branch_id BIGINT NOT NULL REFERENCES branches(id) ON DELETE CASCADE,
  menu_item_id BIGINT NOT NULL REFERENCES menu_items(id) ON DELETE CASCADE,
  is_active BOOLEAN NOT NULL DEFAULT true,
  is_stop_list BOOLEAN NOT NULL DEFAULT false,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (branch_id, menu_item_id)
);

CREATE INDEX idx_branch_menu_item_overrides_branch ON branch_menu_item_overrides(branch_id);
CREATE INDEX idx_branch_menu_item_overrides_item ON branch_menu_item_overrides(menu_item_id);
