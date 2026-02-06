CREATE TABLE menu_time_slots (
  id BIGSERIAL PRIMARY KEY,
  branch_id BIGINT NOT NULL REFERENCES branches(id) ON DELETE CASCADE,
  name TEXT NOT NULL,
  days_mask INT NOT NULL DEFAULT 127,
  start_time TIME NOT NULL,
  end_time TIME NOT NULL,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_menu_time_slots_branch ON menu_time_slots(branch_id);

CREATE TABLE menu_item_time_slots (
  menu_item_id BIGINT NOT NULL REFERENCES menu_items(id) ON DELETE CASCADE,
  time_slot_id BIGINT NOT NULL REFERENCES menu_time_slots(id) ON DELETE CASCADE,
  PRIMARY KEY (menu_item_id, time_slot_id)
);

CREATE INDEX idx_menu_item_time_slots_item ON menu_item_time_slots(menu_item_id);
CREATE INDEX idx_menu_item_time_slots_slot ON menu_item_time_slots(time_slot_id);
