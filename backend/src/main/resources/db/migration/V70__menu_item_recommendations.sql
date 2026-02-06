create table if not exists menu_item_recommendations (
  id bigserial primary key,
  tenant_id bigint not null,
  source_item_id bigint not null,
  target_item_id bigint not null,
  type varchar(32) not null,
  sort_order int not null default 0,
  is_active boolean not null default true,
  created_at timestamptz not null default now()
);

create unique index if not exists uk_menu_item_reco_source_target
  on menu_item_recommendations (source_item_id, target_item_id);

create index if not exists idx_menu_item_reco_source
  on menu_item_recommendations (source_item_id, is_active, sort_order, id);

create index if not exists idx_menu_item_reco_tenant
  on menu_item_recommendations (tenant_id);
