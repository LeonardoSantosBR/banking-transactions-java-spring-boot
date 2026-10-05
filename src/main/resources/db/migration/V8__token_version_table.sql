CREATE TABLE user_token_versions (
    user_id UUID PRIMARY KEY REFERENCES users(id),
    token_version INTEGER NOT NULL DEFAULT 0
);

INSERT INTO user_token_versions (user_id, token_version)
SELECT id, 0 FROM users;