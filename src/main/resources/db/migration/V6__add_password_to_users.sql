-- Existing installations must populate this column before enforcing NOT NULL.
ALTER TABLE users ADD COLUMN password VARCHAR(255) NOT NULL;
