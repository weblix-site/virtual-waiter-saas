create table if not exists menu_tags (
  id bigserial primary key,
  tenant_id bigint not null,
  name varchar(100) not null,
  slug varchar(100) not null,
  is_allergen boolean not null default false,
  is_active boolean not null default true,
  created_at timestamptz not null default now()
);

create unique index if not exists uq_menu_tags_tenant_slug on menu_tags(tenant_id, slug);
create index if not exists ix_menu_tags_tenant on menu_tags(tenant_id);

create table if not exists menu_item_tags (
  menu_item_id bigint not null references menu_items(id) on delete cascade,
  tag_id bigint not null references menu_tags(id) on delete cascade,
  primary key(menu_item_id, tag_id)
);
