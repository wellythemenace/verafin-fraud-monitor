-- Core tables for the transaction monitoring system.
CREATE TABLE IF NOT EXISTS accounts (
    id           BIGSERIAL PRIMARY KEY,
    customer_name VARCHAR(120) NOT NULL,
    opened_at    TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS transactions (
    id           BIGSERIAL PRIMARY KEY,
    account_id   BIGINT NOT NULL REFERENCES accounts(id),
    amount       NUMERIC(14,2) NOT NULL,
    type         VARCHAR(20) NOT NULL,      -- DEPOSIT, WITHDRAWAL, TRANSFER
    occurred_at  TIMESTAMP NOT NULL,
    created_at   TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_tx_account_time ON transactions(account_id, occurred_at);

CREATE TABLE IF NOT EXISTS alerts (
    id             BIGSERIAL PRIMARY KEY,
    transaction_id BIGINT REFERENCES transactions(id),
    account_id     BIGINT NOT NULL REFERENCES accounts(id),
    rule_name      VARCHAR(60) NOT NULL,    -- e.g. STRUCTURING, VELOCITY
    risk_score     NUMERIC(5,2),
    reason         TEXT NOT NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'OPEN',  -- OPEN, REVIEWED, CLOSED
    created_at     TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Customer profile (KYC): what we expect this customer to do
ALTER TABLE accounts ADD COLUMN IF NOT EXISTS risk_rating VARCHAR(10) NOT NULL DEFAULT 'LOW';
ALTER TABLE accounts ADD COLUMN IF NOT EXISTS expected_monthly_deposits NUMERIC(14,2);
ALTER TABLE accounts ADD COLUMN IF NOT EXISTS source_of_funds_note TEXT;
