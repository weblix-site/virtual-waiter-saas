CREATE TABLE tenants (id BIGSERIAL PRIMARY KEY, name TEXT NOT NULL);
CREATE TABLE branches (id BIGSERIAL PRIMARY KEY, tenant_id BIGINT NOT NULL REFERENCES tenants(id), name TEXT NOT NULL);
CREATE TABLE tables (id BIGSERIAL PRIMARY KEY, branch_id BIGINT NOT NULL REFERENCES branches(id), number INT NOT NULL, public_id TEXT NOT NULL UNIQUE);
CREATE TABLE guest_sessions (id BIGSERIAL PRIMARY KEY, table_id BIGINT NOT NULL REFERENCES tables(id), locale TEXT NOT NULL, expires_at TIMESTAMPTZ NOT NULL);
CREATE TABLE menu_categories (id BIGSERIAL PRIMARY KEY, branch_id BIGINT NOT NULL REFERENCES branches(id), name_ru TEXT NOT NULL, name_ro TEXT, name_en TEXT, sort_order INT NOT NULL DEFAULT 0, is_active BOOLEAN NOT NULL DEFAULT TRUE);
CREATE TABLE menu_items (id BIGSERIAL PRIMARY KEY, category_id BIGINT NOT NULL REFERENCES menu_categories(id), name_ru TEXT NOT NULL, name_ro TEXT, name_en TEXT, description_ru TEXT, description_ro TEXT, description_en TEXT, price_cents INT NOT NULL, currency TEXT NOT NULL DEFAULT 'MDL', is_active BOOLEAN NOT NULL DEFAULT TRUE);
CREATE TABLE orders (id BIGSERIAL PRIMARY KEY, table_id BIGINT NOT NULL REFERENCES tables(id), guest_session_id BIGINT NOT NULL REFERENCES guest_sessions(id), status TEXT NOT NULL DEFAULT 'NEW', created_at TIMESTAMPTZ NOT NULL DEFAULT now());
CREATE TABLE order_items (id BIGSERIAL PRIMARY KEY, order_id BIGINT NOT NULL REFERENCES orders(id), menu_item_id BIGINT NOT NULL REFERENCES menu_items(id), name_snapshot TEXT NOT NULL, unit_price_cents INT NOT NULL, qty INT NOT NULL);

INSERT INTO tenants(name) VALUES ('Demo Cafe');
INSERT INTO branches(tenant_id, name) VALUES (1, 'Main');
INSERT INTO tables(branch_id, number, public_id) VALUES (1, 1, 'TBL_DEMO_0001');
INSERT INTO menu_categories(branch_id, name_ru, name_ro, name_en, sort_order) VALUES (1,'Напитки','Băuturi','Drinks',1);
INSERT INTO menu_items(category_id, name_ru, name_ro, name_en, description_ru, price_cents) VALUES (1,'Американо','Americano','Americano','Классический кофе.',3500);
