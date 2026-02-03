create table if not exists hall_plan_versions (
  id bigserial primary key,
  plan_id bigint not null references hall_plans(id) on delete cascade,
  hall_id bigint not null references branch_halls(id) on delete cascade,
  branch_id bigint not null references branches(id) on delete cascade,
  name varchar(255) not null,
  sort_order int not null default 0,
  is_active boolean not null default true,
  layout_bg_url text,
  layout_zones_json text,
  created_at timestamptz not null default now(),
  created_by_staff_id bigint references staff_users(id),
  action varchar(32)
);

create index if not exists idx_hall_plan_versions_plan on hall_plan_versions(plan_id, created_at desc);
