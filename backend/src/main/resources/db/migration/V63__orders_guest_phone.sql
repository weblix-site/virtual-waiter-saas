-- Store guest phone on orders for reporting

alter table orders
  add column if not exists guest_phone varchar(32);

update orders o
set guest_phone = s.verified_phone
from guest_sessions s
where o.guest_session_id = s.id
  and s.verified_phone is not null
  and (o.guest_phone is null or o.guest_phone = '');

create index if not exists idx_orders_guest_phone on orders(guest_phone);
