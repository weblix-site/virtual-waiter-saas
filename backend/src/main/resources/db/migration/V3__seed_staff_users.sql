-- Demo staff users
-- Password for all demo users: demo123

INSERT INTO staff_users(branch_id, username, password_hash, role)
VALUES
  (1, 'waiter1', '$2b$10$/Nx8ebrahAnZP8Ynb3eI4.pzm7N2h2wrBaymXG8u7IkMCR8nDIAPu', 'WAITER')
ON CONFLICT (username) DO NOTHING;

INSERT INTO staff_users(branch_id, username, password_hash, role)
VALUES
  (1, 'kitchen1', '$2b$10$/Nx8ebrahAnZP8Ynb3eI4.pzm7N2h2wrBaymXG8u7IkMCR8nDIAPu', 'KITCHEN')
ON CONFLICT (username) DO NOTHING;
