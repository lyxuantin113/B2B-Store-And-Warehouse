-- ==============================================================================
-- V3__dynamic_price_tier_labels.sql
-- Enhances product_price_tiers so Admins can configure arbitrary quantity ranges
-- (min_qty ~ max_qty), custom tier labels, and optional discount percentages
-- without any hardcoded tier restrictions.
-- ==============================================================================

ALTER TABLE product_price_tiers
    ALTER COLUMN tier_code TYPE VARCHAR(60);

ALTER TABLE product_price_tiers
    ADD COLUMN IF NOT EXISTS discount_percent NUMERIC(5, 2)
        CHECK (discount_percent IS NULL OR (discount_percent >= 0 AND discount_percent <= 100));
