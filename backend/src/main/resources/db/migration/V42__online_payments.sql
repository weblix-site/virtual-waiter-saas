create table if not exists payment_intents (
  id bigserial primary key,
  branch_id bigint not null,
  table_id bigint not null,
  guest_session_id bigint not null,
  bill_request_id bigint,
  provider varchar(16) not null,
  status varchar(24) not null,
  amount_cents int not null,
  currency_code varchar(8) not null,
  items_json text,
  provider_ref varchar(128),
  return_url text,
  cancel_url text,
  created_at timestamp with time zone not null default now(),
  updated_at timestamp with time zone
);

create index if not exists idx_payment_intents_branch on payment_intents(branch_id);
create index if not exists idx_payment_intents_bill on payment_intents(bill_request_id);
create index if not exists idx_payment_intents_provider_ref on payment_intents(provider_ref);

create table if not exists payment_transactions (
  id bigserial primary key,
  intent_id bigint not null,
  provider varchar(16) not null,
  status varchar(24) not null,
  amount_cents int not null,
  provider_ref varchar(128),
  provider_payload text,
  created_at timestamp with time zone not null default now()
);

create index if not exists idx_payment_tx_intent on payment_transactions(intent_id);

alter table branch_settings
  add column if not exists online_pay_enabled boolean;

alter table branch_settings
  add column if not exists online_pay_provider varchar(16);

alter table branch_settings
  add column if not exists online_pay_currency_code varchar(8);
