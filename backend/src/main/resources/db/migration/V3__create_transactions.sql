CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    portfolio_id BIGINT NOT NULL,
    asset_id BIGINT NOT NULL,
    transaction_type VARCHAR(10) NOT NULL,
    quantity NUMERIC(24,8) NOT NULL,
    unit_price NUMERIC(24,8) NOT NULL,
    fee NUMERIC(24,8) NOT NULL DEFAULT 0,
    transaction_date TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_transactions_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolios (id),
    CONSTRAINT fk_transactions_asset FOREIGN KEY (asset_id) REFERENCES assets (id),
    CONSTRAINT chk_transactions_transaction_type CHECK (transaction_type IN ('BUY', 'SELL')),
    CONSTRAINT chk_transactions_quantity_positive CHECK (quantity > 0),
    CONSTRAINT chk_transactions_unit_price_positive CHECK (unit_price > 0),
    CONSTRAINT chk_transactions_fee_not_negative CHECK (fee >= 0)
);

CREATE INDEX idx_transactions_portfolio_id ON transactions (portfolio_id);
CREATE INDEX idx_transactions_asset_id ON transactions (asset_id);
CREATE INDEX idx_transactions_portfolio_id_transaction_date ON transactions (portfolio_id, transaction_date);
