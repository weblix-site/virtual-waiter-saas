-- OTP delivery status metadata

alter table otp_challenges
  add column if not exists delivery_status varchar(16) not null default 'SENT';

alter table otp_challenges
  add column if not exists delivery_provider_ref varchar(64);

alter table otp_challenges
  add column if not exists delivery_error varchar(255);

alter table otp_challenges
  add column if not exists delivered_at timestamptz;
