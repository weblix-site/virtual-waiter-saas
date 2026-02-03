CREATE TABLE IF NOT EXISTS staff_reviews (
  id BIGSERIAL PRIMARY KEY,
  branch_id BIGINT NOT NULL,
  staff_user_id BIGINT NOT NULL,
  guest_session_id BIGINT NOT NULL,
  table_id BIGINT NOT NULL,
  rating INTEGER NOT NULL,
  comment TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT staff_reviews_rating_chk CHECK (rating >= 1 AND rating <= 5),
  CONSTRAINT staff_reviews_guest_session_uq UNIQUE (guest_session_id)
);

CREATE INDEX IF NOT EXISTS idx_staff_reviews_branch ON staff_reviews(branch_id);
CREATE INDEX IF NOT EXISTS idx_staff_reviews_staff ON staff_reviews(staff_user_id);
