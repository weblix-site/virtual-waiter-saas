CREATE TABLE IF NOT EXISTS chat_reads (
  id BIGSERIAL PRIMARY KEY,
  branch_id BIGINT NOT NULL,
  staff_user_id BIGINT NOT NULL,
  guest_session_id BIGINT NOT NULL,
  last_read_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT chat_reads_uq UNIQUE (staff_user_id, guest_session_id)
);

CREATE INDEX IF NOT EXISTS idx_chat_reads_branch ON chat_reads(branch_id);
CREATE INDEX IF NOT EXISTS idx_chat_reads_staff ON chat_reads(staff_user_id);
