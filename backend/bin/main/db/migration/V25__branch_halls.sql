create table if not exists branch_halls (
  id bigserial primary key,
  branch_id bigint not null,
  name varchar(120) not null,
  is_active boolean not null default true,
  sort_order integer not null default 0,
  layout_bg_url text,
  layout_zones_json text
);

alter table tables
  add column if not exists hall_id bigint;
