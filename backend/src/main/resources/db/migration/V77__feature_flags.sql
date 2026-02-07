create table if not exists feature_flags (
  id bigserial primary key,
  code varchar(64) not null unique,
  name varchar(120) not null,
  description varchar(512),
  default_enabled boolean not null default false,
  created_at timestamp not null default now()
);

create table if not exists tenant_feature_flags (
  id bigserial primary key,
  tenant_id bigint not null references tenants(id) on delete cascade,
  flag_id bigint not null references feature_flags(id) on delete cascade,
  enabled boolean not null,
  updated_at timestamp not null default now(),
  unique (tenant_id, flag_id)
);

create table if not exists branch_feature_flags (
  id bigserial primary key,
  branch_id bigint not null references branches(id) on delete cascade,
  flag_id bigint not null references feature_flags(id) on delete cascade,
  enabled boolean not null,
  updated_at timestamp not null default now(),
  unique (branch_id, flag_id)
);

create index if not exists idx_tenant_feature_flags_tenant on tenant_feature_flags(tenant_id);
create index if not exists idx_tenant_feature_flags_flag on tenant_feature_flags(flag_id);
create index if not exists idx_branch_feature_flags_branch on branch_feature_flags(branch_id);
create index if not exists idx_branch_feature_flags_flag on branch_feature_flags(flag_id);
