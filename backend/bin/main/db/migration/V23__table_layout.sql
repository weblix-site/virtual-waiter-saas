alter table tables
  add column if not exists layout_x numeric,
  add column if not exists layout_y numeric,
  add column if not exists layout_w numeric,
  add column if not exists layout_h numeric,
  add column if not exists layout_shape varchar(16),
  add column if not exists layout_rotation integer,
  add column if not exists layout_zone varchar(64);
