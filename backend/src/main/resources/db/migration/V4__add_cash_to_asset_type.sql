ALTER TABLE assets
DROP CONSTRAINT chk_assets_asset_type;

ALTER TABLE assets
ADD CONSTRAINT chk_assets_asset_type
CHECK (asset_type IN ('STOCK', 'CRYPTO', 'FOREX', 'CASH'));
