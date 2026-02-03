alter table branch_halls
  add column if not exists active_plan_id bigint;

create table if not exists hall_plans (
  id bigserial primary key,
  hall_id bigint not null,
  name varchar(120) not null,
  is_active boolean not null default true,
  sort_order integer not null default 0,
  layout_bg_url text,
  layout_zones_json text
);
