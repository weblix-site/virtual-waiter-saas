CREATE TABLE IF NOT EXISTS branch_reviews (
  id BIGSERIAL PRIMARY KEY,
  branch_id BIGINT NOT NULL,
  guest_session_id BIGINT NOT NULL,
  rating INTEGER NOT NULL,
  comment TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT branch_reviews_rating_chk CHECK (rating >= 1 AND rating <= 5),
  CONSTRAINT branch_reviews_guest_session_uq UNIQUE (guest_session_id)
);

CREATE INDEX IF NOT EXISTS idx_branch_reviews_branch ON branch_reviews(branch_id);
