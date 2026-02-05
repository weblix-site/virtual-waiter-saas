-- OTP max resends per session (within TTL window)

alter table branch_settings
  add column if not exists otp_max_resends integer;
