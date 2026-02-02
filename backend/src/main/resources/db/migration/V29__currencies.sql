create table if not exists currencies (
  code varchar(8) primary key,
  name varchar(64) not null,
  symbol varchar(8),
  is_active boolean not null default true
);

insert into currencies(code, name, symbol, is_active)
values ('MDL', 'Moldovan leu', 'L', true)
on conflict (code) do nothing;

alter table branch_settings
  add column if not exists currency_code varchar(8) default 'MDL';
