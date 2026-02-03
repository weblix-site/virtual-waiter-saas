alter table branch_settings
  add column if not exists service_fee_percent int;

alter table branch_settings
  add column if not exists tax_percent int;

alter table bill_requests
  add column if not exists service_fee_percent int;

alter table bill_requests
  add column if not exists service_fee_cents int not null default 0;

alter table bill_requests
  add column if not exists tax_percent int;

alter table bill_requests
  add column if not exists tax_cents int not null default 0;
