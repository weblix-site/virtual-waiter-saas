alter table tenants
  add column if not exists logo_url varchar(512),
  add column if not exists country varchar(128),
  add column if not exists address varchar(255),
  add column if not exists phone varchar(64),
  add column if not exists contact_person varchar(128);

alter table branches
  add column if not exists logo_url varchar(512),
  add column if not exists country varchar(128),
  add column if not exists address varchar(255),
  add column if not exists phone varchar(64),
  add column if not exists contact_person varchar(128);
