alter table branch_settings
  add column if not exists commission_model varchar(32),
  add column if not exists commission_monthly_fixed_cents int,
  add column if not exists commission_monthly_percent int,
  add column if not exists commission_order_percent int,
  add column if not exists commission_order_fixed_cents int;
