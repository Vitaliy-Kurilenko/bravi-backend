-- Explicit SELLER registration: users gain email_verified + a public identifier,
-- and email becomes unique (idempotency / cross-user conflict rule).
ALTER TABLE users
    ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT false;

ALTER TABLE users
    ADD COLUMN public_id VARCHAR(255);

UPDATE users
SET public_id = 'usr_' || id
WHERE public_id IS NULL;

ALTER TABLE users
    ALTER COLUMN public_id SET NOT NULL;

ALTER TABLE users
    ADD CONSTRAINT uc_users_public_id UNIQUE (public_id);

ALTER TABLE users
    ADD CONSTRAINT uc_users_email UNIQUE (email);
