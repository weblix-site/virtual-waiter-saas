-- Stop-list flag for menu items

ALTER TABLE menu_items
  ADD COLUMN IF NOT EXISTS is_stop_list BOOLEAN NOT NULL DEFAULT FALSE;
