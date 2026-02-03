CREATE TABLE IF NOT EXISTS chat_messages (
  id BIGSERIAL PRIMARY KEY,
  branch_id BIGINT NOT NULL,
  table_id BIGINT NOT NULL,
  guest_session_id BIGINT NOT NULL,
  sender_role VARCHAR(20) NOT NULL,
  staff_user_id BIGINT,
  message TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_chat_branch ON chat_messages(branch_id);
CREATE INDEX IF NOT EXISTS idx_chat_guest_session ON chat_messages(guest_session_id);
CREATE INDEX IF NOT EXISTS idx_chat_created_at ON chat_messages(created_at);
