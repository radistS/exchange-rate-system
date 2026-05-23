-- Align currency_code with JPA VARCHAR(3) mapping (existing DBs created with CHAR(3))
ALTER TABLE exchange_rate
    ALTER COLUMN currency_code TYPE VARCHAR(3);

ALTER TABLE currency_usage
    ALTER COLUMN currency_code TYPE VARCHAR(3);
