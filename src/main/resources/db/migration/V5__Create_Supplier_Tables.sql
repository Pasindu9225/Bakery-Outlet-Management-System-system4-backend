-- Create suppliers table
CREATE TABLE IF NOT EXISTS suppliers (
    supplier_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    address VARCHAR(500) DEFAULT NULL,
    contact_number VARCHAR(50) DEFAULT NULL,
    email VARCHAR(100) DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Create raw_material_suppliers table
CREATE TABLE IF NOT EXISTS raw_material_suppliers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    raw_material_id BIGINT NOT NULL,
    supplier_id BIGINT NOT NULL,
    lead_time_days INT DEFAULT NULL,
    negotiated_unit_cost DOUBLE DEFAULT NULL,
    is_preferred BIT(1) DEFAULT b'0',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_raw_material_supplier_material FOREIGN KEY (raw_material_id) REFERENCES raw_materials(id) ON DELETE CASCADE,
    CONSTRAINT fk_raw_material_supplier_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(supplier_id) ON DELETE CASCADE,
    UNIQUE KEY uk_raw_material_supplier (raw_material_id, supplier_id)
);

-- Insert sample suppliers
INSERT INTO suppliers (name, address, contact_number, email) VALUES
('Fresh Ingredients Ltd', '123 Supply Street, Bakery District, City', '+1-555-0101', 'orders@freshingredients.com'),
('Premium Flour Mills', '456 Mill Road, Grain Valley, State', '+1-555-0102', 'sales@premiumflour.com'),
('Quality Dairy Co', '789 Dairy Lane, Farmland, County', '+1-555-0103', 'supply@qualitydairy.com'),
('Organic Supplies Inc', '321 Natural Way, Green Hills, Province', '+1-555-0104', 'info@organicsupplies.com'),
('Bulk Ingredients Corp', '654 Wholesale Blvd, Industrial Zone', '+1-555-0105', 'bulk@ingredients.com');

-- Insert sample raw_material_suppliers relationships
-- Note: These raw_material_id values should match existing raw materials (1-10 based on AUTO_INCREMENT=11)
INSERT INTO raw_material_suppliers (raw_material_id, supplier_id, lead_time_days, negotiated_unit_cost, is_preferred) VALUES
-- Flour suppliers
(1, 1, 3, 2.45, b'1'), -- Fresh Ingredients - preferred
(1, 2, 5, 2.30, b'0'), -- Premium Flour Mills - better price but longer lead time
(1, 5, 7, 2.25, b'0'), -- Bulk Ingredients - cheapest but longest lead time

-- Sugar suppliers  
(2, 1, 2, 1.15, b'1'), -- Fresh Ingredients - preferred
(2, 4, 4, 1.20, b'0'), -- Organic Supplies
(2, 5, 6, 1.10, b'0'), -- Bulk Ingredients - cheapest

-- Milk suppliers
(3, 3, 1, 0.85, b'1'), -- Quality Dairy - preferred (fastest delivery)
(3, 1, 3, 0.90, b'0'), -- Fresh Ingredients

-- Butter suppliers
(4, 3, 2, 4.25, b'1'), -- Quality Dairy - preferred
(4, 4, 5, 4.50, b'0'), -- Organic Supplies - organic premium
(4, 1, 3, 4.30, b'0'), -- Fresh Ingredients

-- Eggs suppliers
(5, 3, 1, 2.80, b'1'), -- Quality Dairy - preferred
(5, 4, 3, 3.20, b'0'), -- Organic Supplies - organic premium
(5, 1, 2, 2.85, b'0'), -- Fresh Ingredients

-- Salt suppliers
(6, 1, 5, 0.45, b'1'), -- Fresh Ingredients - preferred
(6, 5, 10, 0.35, b'0'), -- Bulk Ingredients - bulk pricing

-- Yeast suppliers
(7, 1, 2, 8.50, b'1'), -- Fresh Ingredients - preferred
(7, 2, 4, 8.25, b'0'), -- Premium Flour Mills
(7, 5, 7, 8.00, b'0'), -- Bulk Ingredients - cheapest

-- Oil suppliers
(8, 1, 3, 3.20, b'1'), -- Fresh Ingredients - preferred
(8, 4, 5, 3.50, b'0'), -- Organic Supplies - organic premium
(8, 5, 8, 3.00, b'0'), -- Bulk Ingredients - cheapest

-- Vanilla suppliers
(9, 1, 4, 12.50, b'1'), -- Fresh Ingredients - preferred
(9, 4, 6, 15.00, b'0'), -- Organic Supplies - organic premium

-- Cocoa suppliers
(10, 1, 5, 6.80, b'1'), -- Fresh Ingredients - preferred
(10, 4, 7, 7.20, b'0'), -- Organic Supplies - organic premium
(10, 5, 10, 6.50, b'0'); -- Bulk Ingredients - cheapest
