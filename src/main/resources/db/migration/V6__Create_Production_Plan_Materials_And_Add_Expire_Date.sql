-- Add expire_date column to raw_materials table
ALTER TABLE raw_materials ADD COLUMN expire_date DATE;

-- Create production_plan_materials table
CREATE TABLE IF NOT EXISTS production_plan_materials (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    production_plan_id BIGINT NOT NULL,
    raw_material_id BIGINT NOT NULL,
    total_raw_material_quantity DECIMAL(10,3) NOT NULL,
    raw_material_quantity_for_kitchen DECIMAL(10,3),
    raw_material_quantity_for_bakery DECIMAL(10,3),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (production_plan_id) REFERENCES production_plans(id) ON DELETE CASCADE,
    FOREIGN KEY (raw_material_id) REFERENCES raw_materials(id)
);

-- Create indexes for better performance
CREATE INDEX idx_production_plan_materials_plan ON production_plan_materials(production_plan_id);
CREATE INDEX idx_production_plan_materials_material ON production_plan_materials(raw_material_id);

-- Insert some sample data for production_plan_materials if production plans exist
-- This assumes production plans with IDs 1, 2 exist and raw materials with IDs 1-5 exist
INSERT INTO production_plan_materials (production_plan_id, raw_material_id, total_raw_material_quantity, raw_material_quantity_for_kitchen, raw_material_quantity_for_bakery) VALUES
(1, 1, 75.0, 25.0, 50.0),   -- All Purpose Flour
(1, 3, 15.0, 5.0, 10.0),    -- Sugar
(1, 4, 8.0, 3.0, 5.0),      -- Butter
(1, 5, 50, 20, 30),         -- Eggs
(2, 1, 50.0, 20.0, 30.0),   -- All Purpose Flour
(2, 2, 25.0, 10.0, 15.0),   -- Whole Wheat Flour
(2, 3, 10.0, 4.0, 6.0);     -- Sugar

-- Update some raw materials with sample expire dates
UPDATE raw_materials SET expire_date = '2025-12-15' WHERE id = 1;  -- All Purpose Flour
UPDATE raw_materials SET expire_date = '2025-11-30' WHERE id = 2;  -- Whole Wheat Flour
UPDATE raw_materials SET expire_date = '2026-06-30' WHERE id = 3;  -- Sugar
UPDATE raw_materials SET expire_date = '2025-10-20' WHERE id = 4;  -- Butter
UPDATE raw_materials SET expire_date = '2025-09-25' WHERE id = 5;  -- Eggs
UPDATE raw_materials SET expire_date = '2025-10-15' WHERE id = 6;  -- Milk
UPDATE raw_materials SET expire_date = '2027-03-01' WHERE id = 7;  -- Yeast
UPDATE raw_materials SET expire_date = '2030-01-01' WHERE id = 8;  -- Salt (no expiry)
UPDATE raw_materials SET expire_date = '2026-08-15' WHERE id = 9;  -- Chocolate Chips
UPDATE raw_materials SET expire_date = '2028-05-20' WHERE id = 10; -- Vanilla Extract