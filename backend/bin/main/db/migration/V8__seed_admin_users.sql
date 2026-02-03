-- Demo admin users
-- Password: demo123

INSERT INTO staff_users(branch_id, username, password_hash, role)
VALUES
  (1, 'admin1', '$2b$10$/Nx8ebrahAnZP8Ynb3eI4.pzm7N2h2wrBaymXG8u7IkMCR8nDIAPu', 'ADMIN')
ON CONFLICT (username) DO NOTHING;

INSERT INTO staff_users(branch_id, username, password_hash, role)
VALUES
  (NULL, 'superadmin', '$2b$10$/Nx8ebrahAnZP8Ynb3eI4.pzm7N2h2wrBaymXG8u7IkMCR8nDIAPu', 'SUPER_ADMIN')
ON CONFLICT (username) DO NOTHING;
