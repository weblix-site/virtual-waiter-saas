create table if not exists restaurants (
  id bigserial primary key,
  tenant_id bigint not null references tenants(id) on delete cascade,
  name varchar(255) not null,
  logo_url varchar(512),
  country varchar(128),
  address varchar(255),
  phone varchar(64),
  contact_person varchar(128),
  is_active boolean not null default true
);

insert into restaurants (tenant_id, name, logo_url, country, address, phone, contact_person, is_active)
select t.id, t.name, t.logo_url, t.country, t.address, t.phone, t.contact_person, t.is_active
from tenants t;

alter table branches
  add column if not exists restaurant_id bigint;

update branches b
set restaurant_id = r.id
from restaurants r
where b.restaurant_id is null
  and r.tenant_id = b.tenant_id
  and r.id = (
    select min(r2.id) from restaurants r2 where r2.tenant_id = b.tenant_id
  );

alter table branches
  alter column restaurant_id set not null;

do $$
begin
  if not exists (
    select 1 from pg_constraint where conname = 'fk_branches_restaurant'
  ) then
    alter table branches
      add constraint fk_branches_restaurant
      foreign key (restaurant_id) references restaurants(id) on delete restrict;
  end if;
end $$;
