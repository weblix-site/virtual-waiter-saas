create table if not exists guest_flag_audit (
  id bigserial primary key,
  phone_e164 varchar(32) not null,
  branch_id bigint,
  flag_type varchar(32) not null,
  old_active boolean,
  new_active boolean not null,
  changed_by_staff_id bigint,
  changed_at timestamp not null default now()
);

create index if not exists ix_guest_flag_audit_phone on guest_flag_audit(phone_e164);
