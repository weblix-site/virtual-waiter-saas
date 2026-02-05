-- Guest profiles (phone-based)

create table if not exists guest_profiles (
  id bigserial primary key,
  phone_e164 varchar(32) not null unique,
  name varchar(120),
  preferences text,
  allergens text,
  visits_count integer not null default 0,
  first_visit_at timestamptz,
  last_visit_at timestamptz
);
