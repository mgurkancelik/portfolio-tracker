CREATE TABLE portfolios (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    base_currency VARCHAR(3) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_portfolios_name_not_blank CHECK (length(trim(name)) > 0),
    CONSTRAINT chk_portfolios_base_currency_length CHECK (char_length(base_currency) = 3)
);
