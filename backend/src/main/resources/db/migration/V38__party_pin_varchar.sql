-- Fix pin column type to match entity (varchar(4))
ALTER TABLE table_parties
  ALTER COLUMN pin TYPE varchar(4);
