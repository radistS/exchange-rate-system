-- rate_date = Fixer API business date (not ingestion timestamp)
CREATE TABLE exchange_rate (
    id              BIGSERIAL PRIMARY KEY,
    currency_code   CHAR(3)         NOT NULL,
    rate_to_usd     NUMERIC(19, 8)  NOT NULL,
    rate_date       DATE            NOT NULL,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_exchange_rate_currency_date UNIQUE (currency_code, rate_date)
);

CREATE INDEX idx_exchange_rate_date ON exchange_rate (rate_date);

CREATE TABLE currency_usage (
    currency_code   CHAR(3) PRIMARY KEY,
    query_count     BIGINT      NOT NULL DEFAULT 0,
    last_queried_at DATE
);
