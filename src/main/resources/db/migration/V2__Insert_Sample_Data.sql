-- Insert sample products
INSERT INTO products (product_name, product_code, description, unit_price, category, is_active) VALUES
('White Bread', 'WB001', 'Fresh white bread', 2.50, 'Bread', true),
('Whole Wheat Bread', 'WWB001', 'Healthy whole wheat bread', 3.00, 'Bread', true),
('Croissant', 'CR001', 'Buttery croissant', 1.50, 'Pastry', true),
('Chocolate Cake', 'CC001', 'Delicious chocolate cake', 25.00, 'Cake', true),
('Donut', 'DN001', 'Glazed donut', 1.00, 'Pastry', true);

-- Insert sample raw materials
INSERT INTO raw_materials (material_name, material_code, description, unit_of_measure, unit_cost, current_stock, minimum_stock_level, category, is_active) VALUES
('All Purpose Flour', 'APF001', 'All purpose wheat flour', 'kg', 1.20, 100.0, 20.0, 'Flour', true),
('Whole Wheat Flour', 'WWF001', 'Whole wheat flour', 'kg', 1.50, 50.0, 10.0, 'Flour', true),
('Sugar', 'SUG001', 'Granulated white sugar', 'kg', 0.80, 75.0, 15.0, 'Sweetener', true),
('Butter', 'BUT001', 'Unsalted butter', 'kg', 8.00, 25.0, 5.0, 'Dairy', true),
('Eggs', 'EGG001', 'Fresh chicken eggs', 'piece', 0.25, 200.0, 40.0, 'Dairy', true),
('Milk', 'MIL001', 'Fresh whole milk', 'liter', 1.20, 30.0, 6.0, 'Dairy', true),
('Yeast', 'YEA001', 'Active dry yeast', 'kg', 15.00, 5.0, 1.0, 'Leavening', true),
('Salt', 'SAL001', 'Table salt', 'kg', 0.50, 10.0, 2.0, 'Seasoning', true),
('Chocolate Chips', 'CHC001', 'Semi-sweet chocolate chips', 'kg', 12.00, 15.0, 3.0, 'Chocolate', true),
('Vanilla Extract', 'VAN001', 'Pure vanilla extract', 'liter', 25.00, 2.0, 0.5, 'Flavoring', true);

-- Insert sample recipes
INSERT INTO recipes (product_id, recipe_name, version, is_active, total_cost_per_unit) VALUES
(1, 'White Bread Recipe', '1.0', true, 1.20),
(2, 'Whole Wheat Bread Recipe', '1.0', true, 1.45),
(3, 'Croissant Recipe', '1.0', true, 0.85),
(4, 'Chocolate Cake Recipe', '1.0', true, 12.50),
(5, 'Donut Recipe', '1.0', true, 0.45);

-- Insert recipe ingredients for White Bread
INSERT INTO recipe_ingredients (recipe_id, raw_material_id, quantity_per_unit, unit_of_measure, cost_per_unit, total_cost_per_unit) VALUES
(1, 1, 0.5, 'kg', 1.20, 0.60),    -- 0.5kg flour
(1, 3, 0.05, 'kg', 0.80, 0.04),   -- 50g sugar
(1, 5, 1, 'piece', 0.25, 0.25),   -- 1 egg
(1, 6, 0.3, 'liter', 1.20, 0.36), -- 300ml milk
(1, 7, 0.01, 'kg', 15.00, 0.15),  -- 10g yeast
(1, 8, 0.01, 'kg', 0.50, 0.005);  -- 10g salt

-- Insert recipe ingredients for Whole Wheat Bread
INSERT INTO recipe_ingredients (recipe_id, raw_material_id, quantity_per_unit, unit_of_measure, cost_per_unit, total_cost_per_unit) VALUES
(2, 2, 0.6, 'kg', 1.50, 0.90),    -- 0.6kg whole wheat flour
(2, 3, 0.05, 'kg', 0.80, 0.04),   -- 50g sugar
(2, 5, 1, 'piece', 0.25, 0.25),   -- 1 egg
(2, 6, 0.35, 'liter', 1.20, 0.42), -- 350ml milk
(2, 7, 0.01, 'kg', 15.00, 0.15),  -- 10g yeast
(2, 8, 0.01, 'kg', 0.50, 0.005);  -- 10g salt

-- Insert recipe ingredients for Croissant
INSERT INTO recipe_ingredients (recipe_id, raw_material_id, quantity_per_unit, unit_of_measure, cost_per_unit, total_cost_per_unit) VALUES
(3, 1, 0.15, 'kg', 1.20, 0.18),   -- 150g flour
(3, 4, 0.08, 'kg', 8.00, 0.64),   -- 80g butter
(3, 3, 0.02, 'kg', 0.80, 0.016),  -- 20g sugar
(3, 5, 0.5, 'piece', 0.25, 0.125), -- 0.5 egg
(3, 6, 0.05, 'liter', 1.20, 0.06), -- 50ml milk
(3, 7, 0.005, 'kg', 15.00, 0.075); -- 5g yeast

-- Insert recipe ingredients for Chocolate Cake
INSERT INTO recipe_ingredients (recipe_id, raw_material_id, quantity_per_unit, unit_of_measure, cost_per_unit, total_cost_per_unit) VALUES
(4, 1, 0.4, 'kg', 1.20, 0.48),    -- 400g flour
(4, 3, 0.3, 'kg', 0.80, 0.24),    -- 300g sugar
(4, 4, 0.25, 'kg', 8.00, 2.00),   -- 250g butter
(4, 5, 4, 'piece', 0.25, 1.00),   -- 4 eggs
(4, 6, 0.4, 'liter', 1.20, 0.48), -- 400ml milk
(4, 9, 0.2, 'kg', 12.00, 2.40),  -- 200g chocolate chips
(4, 10, 0.01, 'liter', 25.00, 0.25); -- 10ml vanilla

-- Insert recipe ingredients for Donut
INSERT INTO recipe_ingredients (recipe_id, raw_material_id, quantity_per_unit, unit_of_measure, cost_per_unit, total_cost_per_unit) VALUES
(5, 1, 0.1, 'kg', 1.20, 0.12),    -- 100g flour
(5, 3, 0.02, 'kg', 0.80, 0.016),  -- 20g sugar
(5, 4, 0.03, 'kg', 8.00, 0.24),   -- 30g butter
(5, 5, 0.25, 'piece', 0.25, 0.0625), -- 0.25 egg
(5, 6, 0.05, 'liter', 1.20, 0.06), -- 50ml milk
(5, 7, 0.005, 'kg', 15.00, 0.075); -- 5g yeast
