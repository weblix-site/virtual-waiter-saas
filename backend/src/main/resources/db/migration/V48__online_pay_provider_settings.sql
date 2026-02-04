alter table branch_settings
  add column if not exists online_pay_request_url text;

alter table branch_settings
  add column if not exists online_pay_cacert_path text;

alter table branch_settings
  add column if not exists online_pay_pcert_path text;

alter table branch_settings
  add column if not exists online_pay_pcert_password text;

alter table branch_settings
  add column if not exists online_pay_key_path text;

alter table branch_settings
  add column if not exists online_pay_redirect_url text;

alter table branch_settings
  add column if not exists online_pay_return_url text;
