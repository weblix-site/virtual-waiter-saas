-- Loyalty / CRM

ALTER TABLE branch_settings
  ADD COLUMN IF NOT EXISTS loyalty_enabled boolean,
  ADD COLUMN IF NOT EXISTS loyalty_points_per_100cents integer;

CREATE TABLE IF NOT EXISTS loyalty_accounts (
  id bigserial PRIMARY KEY,
  branch_id bigint NOT NULL,
  phone varchar(32) NOT NULL,
  points_balance integer NOT NULL DEFAULT 0,
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  updated_at timestamp with time zone
);
CREATE INDEX IF NOT EXISTS idx_loyalty_accounts_branch_phone ON loyalty_accounts(branch_id, phone);

CREATE TABLE IF NOT EXISTS loyalty_points_log (
  id bigserial PRIMARY KEY,
  branch_id bigint NOT NULL,
  phone varchar(32) NOT NULL,
  bill_request_id bigint,
  delta_points integer NOT NULL,
  reason varchar(64),
  created_at timestamp with time zone NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_loyalty_points_bill ON loyalty_points_log(bill_request_id);

CREATE TABLE IF NOT EXISTS guest_favorite_items (
  id bigserial PRIMARY KEY,
  branch_id bigint NOT NULL,
  phone varchar(32) NOT NULL,
  menu_item_id bigint NOT NULL,
  qty_total integer NOT NULL DEFAULT 0,
  last_order_at timestamp with time zone
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_favorites_branch_phone_item ON guest_favorite_items(branch_id, phone, menu_item_id);

CREATE TABLE IF NOT EXISTS guest_offers (
  id bigserial PRIMARY KEY,
  branch_id bigint NOT NULL,
  phone varchar(32) NOT NULL,
  title varchar(120) NOT NULL,
  body text,
  discount_code varchar(32),
  starts_at timestamp with time zone,
  ends_at timestamp with time zone,
  is_active boolean NOT NULL DEFAULT true,
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  updated_at timestamp with time zone
);
CREATE INDEX IF NOT EXISTS idx_guest_offers_branch_phone ON guest_offers(branch_id, phone);
