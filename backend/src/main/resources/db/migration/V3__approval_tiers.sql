-- Migration V3: Multi-tier approval routing support

ALTER TABLE asset_categories ADD COLUMN IF NOT EXISTS required_approval_tier text NOT NULL DEFAULT 'TIER_1_OFFICER';
ALTER TABLE assets ADD COLUMN IF NOT EXISTS approval_tier text NOT NULL DEFAULT 'TIER_1_OFFICER';

ALTER TABLE reservations ADD COLUMN IF NOT EXISTS required_approval_tier text NOT NULL DEFAULT 'TIER_1_OFFICER';
ALTER TABLE reservations ADD COLUMN IF NOT EXISTS current_approval_step text NOT NULL DEFAULT 'PENDING_LEVEL_1';
ALTER TABLE reservations ADD COLUMN IF NOT EXISTS level1_approved_by uuid REFERENCES users (id);
ALTER TABLE reservations ADD COLUMN IF NOT EXISTS level1_approved_at timestamptz;
ALTER TABLE reservations ADD COLUMN IF NOT EXISTS level2_approved_by uuid REFERENCES users (id);
ALTER TABLE reservations ADD COLUMN IF NOT EXISTS level2_approved_at timestamptz;

-- Update existing high value or external use assets & categories in seed
UPDATE asset_categories SET required_approval_tier = 'TIER_2_TECHNICAL' WHERE code IN ('ANALYTICAL_EQUIPMENT', 'OPTICAL_INSTRUMENTS');
UPDATE asset_categories SET required_approval_tier = 'TIER_3_HOD' WHERE code IN ('HEAVY_MACHINERY', 'LAB_VENUES');
