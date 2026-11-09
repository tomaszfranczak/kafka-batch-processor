CREATE TABLE transactions (
    transaction_id TEXT PRIMARY KEY,
    order_id       TEXT        NOT NULL,
    account_id     TEXT        NOT NULL,
    symbol         TEXT        NOT NULL,
    side           TEXT        NOT NULL CHECK (side IN ('BUY', 'SELL')),
    price          NUMERIC     NOT NULL CHECK (price > 0),
    quantity       NUMERIC     NOT NULL CHECK (quantity > 0),
    execution_time TIMESTAMPTZ NOT NULL,
    processed_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
