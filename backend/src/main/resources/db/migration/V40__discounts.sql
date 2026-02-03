create table if not exists branch_discounts (
  id bigserial primary key,
  branch_id bigint not null,
  scope varchar(16) not null default 'COUPON',
  code varchar(32),
  type varchar(16) not null,
  value int not null,
  label varchar(64),
  active boolean not null default true,
  max_uses int,
  used_count int not null default 0,
  starts_at timestamp with time zone,
  ends_at timestamp with time zone,
  days_mask int,
  start_minute int,
  end_minute int,
  tz_offset_minutes int,
  created_at timestamp with time zone not null default now()
);

create index if not exists idx_branch_discounts_branch_code on branch_discounts(branch_id, lower(code));
create index if not exists idx_branch_discounts_branch_scope on branch_discounts(branch_id, scope);

alter table bill_requests
  add column if not exists discount_cents int not null default 0;

alter table bill_requests
  add column if not exists discount_code varchar(32);

alter table bill_requests
  add column if not exists discount_label varchar(64);
