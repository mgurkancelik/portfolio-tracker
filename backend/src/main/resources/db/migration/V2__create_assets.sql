CREATE TABLE assets (
    id BIGSERIAL PRIMARY KEY,
    symbol VARCHAR(20) NOT NULL,
    name VARCHAR(150) NOT NULL,
    asset_type VARCHAR(20) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_assets_symbol_not_blank CHECK (length(trim(symbol)) > 0),
    CONSTRAINT chk_assets_name_not_blank CHECK (length(trim(name)) > 0),
    CONSTRAINT chk_assets_currency_length CHECK (char_length(currency) = 3),
    CONSTRAINT chk_assets_asset_type CHECK (asset_type IN ('STOCK', 'CRYPTO', 'FOREX')),
    CONSTRAINT uk_assets_symbol_asset_type UNIQUE (symbol, asset_type)
);
