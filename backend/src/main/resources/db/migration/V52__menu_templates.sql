CREATE TABLE IF NOT EXISTS menu_templates (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
  restaurant_id BIGINT NULL REFERENCES restaurants(id) ON DELETE CASCADE,
  name TEXT NOT NULL,
  payload_json TEXT NOT NULL,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT now(),
  updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_menu_templates_tenant ON menu_templates(tenant_id);
CREATE INDEX IF NOT EXISTS idx_menu_templates_restaurant ON menu_templates(restaurant_id);

ALTER TABLE branches
  ADD COLUMN IF NOT EXISTS menu_template_id BIGINT REFERENCES menu_templates(id);
