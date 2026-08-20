ALTER TABLE portfolios
    ADD COLUMN user_id BIGINT;

INSERT INTO users (email, password_hash)
SELECT 'legacy@portfolio-tracker.local', 'legacy-user-placeholder'
WHERE EXISTS (
    SELECT 1
    FROM portfolios
    WHERE user_id IS NULL
)
AND NOT EXISTS (
    SELECT 1
    FROM users
    WHERE email = 'legacy@portfolio-tracker.local'
);

UPDATE portfolios
SET user_id = (
    SELECT id
    FROM users
    WHERE email = 'legacy@portfolio-tracker.local'
)
WHERE user_id IS NULL;

ALTER TABLE portfolios
    ALTER COLUMN user_id SET NOT NULL;

ALTER TABLE portfolios
    ADD CONSTRAINT fk_portfolios_user
    FOREIGN KEY (user_id)
    REFERENCES users (id);

CREATE INDEX idx_portfolios_user_id ON portfolios (user_id);
