CREATE TABLE IF NOT EXISTS hall_plan_templates (
  id BIGSERIAL PRIMARY KEY,
  branch_id BIGINT NOT NULL,
  hall_id BIGINT NOT NULL,
  name VARCHAR(120) NOT NULL,
  payload_json TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_hall_plan_templates_branch_hall ON hall_plan_templates(branch_id, hall_id);
