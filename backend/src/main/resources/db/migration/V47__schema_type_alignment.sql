-- Safety alignment: ensure column types match JPA expectations
ALTER TABLE table_parties
  ALTER COLUMN pin TYPE varchar(4);

ALTER TABLE tables
  ALTER COLUMN layout_x TYPE double precision USING layout_x::double precision,
  ALTER COLUMN layout_y TYPE double precision USING layout_y::double precision,
  ALTER COLUMN layout_w TYPE double precision USING layout_w::double precision,
  ALTER COLUMN layout_h TYPE double precision USING layout_h::double precision;
