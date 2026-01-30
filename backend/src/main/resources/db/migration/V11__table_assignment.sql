-- Assign waiter to table

ALTER TABLE tables
  ADD COLUMN IF NOT EXISTS assigned_waiter_id BIGINT NULL REFERENCES staff_users(id);
