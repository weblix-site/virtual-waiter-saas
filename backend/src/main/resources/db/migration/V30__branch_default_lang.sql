ALTER TABLE branch_settings
ADD COLUMN IF NOT EXISTS default_lang VARCHAR(8);
