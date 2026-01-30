-- Audit logs for admin actions

CREATE TABLE IF NOT EXISTS audit_logs (
  id BIGSERIAL PRIMARY KEY,
  actor_user_id BIGINT NULL REFERENCES staff_users(id),
  actor_username TEXT NULL,
  actor_role TEXT NULL,
  branch_id BIGINT NULL REFERENCES branches(id),
  action TEXT NOT NULL,
  entity_type TEXT NOT NULL,
  entity_id BIGINT NULL,
  details_json TEXT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_branch_id_id ON audit_logs(branch_id, id);
