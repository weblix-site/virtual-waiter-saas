ALTER TABLE otp_challenges
  ADD COLUMN IF NOT EXISTS channel varchar(16) NOT NULL DEFAULT 'SMS';

CREATE INDEX IF NOT EXISTS idx_otp_challenges_channel ON otp_challenges(channel);
