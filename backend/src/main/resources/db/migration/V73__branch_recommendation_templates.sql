create table if not exists branch_recommendation_templates (
  id bigserial primary key,
  tenant_id bigint not null,
  branch_id bigint not null,
  name varchar(120) not null,
  sort_order int not null default 0,
  is_active boolean not null default true,
  created_at timestamptz not null default now()
);

create table if not exists branch_recommendation_template_items (
  id bigserial primary key,
  template_id bigint not null references branch_recommendation_templates(id) on delete cascade,
  menu_item_id bigint not null,
  sort_order int not null default 0,
  is_active boolean not null default true
);

create index if not exists idx_brt_templates_branch on branch_recommendation_templates (branch_id, is_active, sort_order, id);
create index if not exists idx_brt_templates_tenant on branch_recommendation_templates (tenant_id);
create index if not exists idx_brt_items_template on branch_recommendation_template_items (template_id, sort_order, id);
create index if not exists idx_brt_items_menu_item on branch_recommendation_template_items (menu_item_id);
