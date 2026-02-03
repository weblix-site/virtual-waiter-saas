-- Staff device tokens (for push)

CREATE TABLE IF NOT EXISTS staff_device_tokens (
  id BIGSERIAL PRIMARY KEY,
  staff_user_id BIGINT NOT NULL REFERENCES staff_users(id) ON DELETE CASCADE,
  branch_id BIGINT NOT NULL REFERENCES branches(id),
  platform TEXT NOT NULL,
  token TEXT NOT NULL UNIQUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  last_seen_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_staff_device_tokens_branch_id ON staff_device_tokens(branch_id);
