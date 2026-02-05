create table if not exists guest_flags (
  id bigserial primary key,
  phone_e164 varchar(32) not null,
  branch_id bigint,
  flag_type varchar(32) not null,
  is_active boolean not null default true,
  created_at timestamp not null default now(),
  updated_at timestamp not null default now()
);

create unique index if not exists ux_guest_flags_phone_branch_type
  on guest_flags(phone_e164, branch_id, flag_type);

create index if not exists ix_guest_flags_phone on guest_flags(phone_e164);
