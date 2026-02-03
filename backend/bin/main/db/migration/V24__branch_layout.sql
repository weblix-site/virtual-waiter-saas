alter table branches
  add column if not exists layout_bg_url text,
  add column if not exists layout_zones_json text;
