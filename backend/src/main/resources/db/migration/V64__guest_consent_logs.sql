-- Guest consent logs (privacy/marketing)

create table if not exists guest_consent_logs (
  id bigserial primary key,
  guest_session_id bigint not null,
  phone_e164 varchar(32) not null,
  branch_id bigint,
  consent_type varchar(32) not null,
  accepted boolean not null,
  text_version varchar(64) not null,
  ip varchar(64),
  user_agent text,
  created_at timestamptz not null default now()
);

create index if not exists idx_guest_consent_phone on guest_consent_logs(phone_e164);
create index if not exists idx_guest_consent_branch on guest_consent_logs(branch_id);
create index if not exists idx_guest_consent_created on guest_consent_logs(created_at);
