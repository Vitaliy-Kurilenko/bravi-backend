-- Seller onboarding profile — 1:1 extension of an ACCOUNT of type SELLER.
CREATE TABLE seller_accounts
(
    account_id        BIGINT                      NOT NULL,
    legal_name        VARCHAR(255),
    onboarding_status VARCHAR(255)                NOT NULL,
    contact_email     VARCHAR(255),
    phone             VARCHAR(64),
    created_at        TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at        TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_seller_accounts PRIMARY KEY (account_id)
);

ALTER TABLE seller_accounts
    ADD CONSTRAINT fk_seller_accounts_on_account FOREIGN KEY (account_id) REFERENCES accounts (id);
