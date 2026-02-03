-- OTP challenges + guest session verification

create table if not exists otp_challenges (
  id bigserial primary key,
  guest_session_id bigint not null references guest_sessions(id) on delete cascade,
  phone_e164 varchar(32) not null,
  otp_hash varchar(255) not null,
  expires_at timestamptz not null,
  attempts_left integer not null,
  status varchar(16) not null,
  created_at timestamptz not null
);

alter table guest_sessions
  add column if not exists is_verified boolean not null default false;

alter table guest_sessions
  add column if not exists verified_phone varchar(32);
