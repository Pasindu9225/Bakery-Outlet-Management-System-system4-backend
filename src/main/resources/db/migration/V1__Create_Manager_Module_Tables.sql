-- Create products table
CREATE TABLE IF NOT EXISTS products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_name VARCHAR(255) NOT NULL UNIQUE,
    product_code VARCHAR(100) UNIQUE,
    description TEXT,
    unit_price DECIMAL(10,2) NOT NULL,
    category VARCHAR(100),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Create raw_materials table
CREATE TABLE IF NOT EXISTS raw_materials (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    material_name VARCHAR(255) NOT NULL UNIQUE,
    material_code VARCHAR(100) UNIQUE,
    description TEXT,
    unit_of_measure VARCHAR(50) NOT NULL,
    unit_cost DECIMAL(10,2) NOT NULL,
    current_stock DECIMAL(10,3) NOT NULL DEFAULT 0,
    minimum_stock_level DECIMAL(10,3) DEFAULT 0,
    category VARCHAR(100),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Create recipes table
CREATE TABLE IF NOT EXISTS recipes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    recipe_name VARCHAR(255) NOT NULL,
    version VARCHAR(50),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    total_cost_per_unit DECIMAL(10,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(id)
);

-- Create recipe_ingredients table
CREATE TABLE IF NOT EXISTS recipe_ingredients (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipe_id BIGINT NOT NULL,
    raw_material_id BIGINT NOT NULL,
    quantity_per_unit DECIMAL(10,3) NOT NULL,
    unit_of_measure VARCHAR(50) NOT NULL,
    cost_per_unit DECIMAL(10,2),
    total_cost_per_unit DECIMAL(10,2),
    FOREIGN KEY (recipe_id) REFERENCES recipes(id),
    FOREIGN KEY (raw_material_id) REFERENCES raw_materials(id)
);

-- Create production_plans table
CREATE TABLE IF NOT EXISTS production_plans (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_name VARCHAR(255) NOT NULL,
    plan_date TIMESTAMP NOT NULL,
    status ENUM('DRAFT', 'APPROVED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED') NOT NULL DEFAULT 'DRAFT',
    total_estimated_cost DECIMAL(10,2),
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Create production_plan_items table
CREATE TABLE IF NOT EXISTS production_plan_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    production_plan_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    quantity INT NOT NULL,
    unit_cost DECIMAL(10,2),
    total_cost DECIMAL(10,2),
    estimated_raw_material_cost DECIMAL(10,2),
    FOREIGN KEY (production_plan_id) REFERENCES production_plans(id) ON DELETE CASCADE
);

-- Create production_orders table
CREATE TABLE IF NOT EXISTS production_orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    production_plan_id BIGINT NOT NULL,
    order_number VARCHAR(100) NOT NULL UNIQUE,
    order_date TIMESTAMP NOT NULL,
    planned_start_date TIMESTAMP,
    planned_end_date TIMESTAMP,
    status ENUM('PENDING', 'APPROVED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED') NOT NULL DEFAULT 'PENDING',
    total_raw_material_cost DECIMAL(10,2),
    total_production_cost DECIMAL(10,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (production_plan_id) REFERENCES production_plans(id)
);

-- Create production_order_items table
CREATE TABLE IF NOT EXISTS production_order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    production_order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    planned_quantity INT NOT NULL,
    completed_quantity INT DEFAULT 0,
    unit_cost DECIMAL(10,2),
    total_cost DECIMAL(10,2),
    raw_material_cost DECIMAL(10,2),
    FOREIGN KEY (production_order_id) REFERENCES production_orders(id) ON DELETE CASCADE
);

-- Create raw_material_requirements table
CREATE TABLE IF NOT EXISTS raw_material_requirements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    production_order_id BIGINT NOT NULL,
    raw_material_id BIGINT NOT NULL,
    required_quantity DECIMAL(10,3) NOT NULL,
    unit_of_measure VARCHAR(50) NOT NULL,
    unit_cost DECIMAL(10,2) NOT NULL,
    total_cost DECIMAL(10,2) NOT NULL,
    available_stock DECIMAL(10,3),
    stock_deficit DECIMAL(10,3),
    FOREIGN KEY (production_order_id) REFERENCES production_orders(id) ON DELETE CASCADE,
    FOREIGN KEY (raw_material_id) REFERENCES raw_materials(id)
);

-- Create indexes for better performance
CREATE INDEX idx_products_name ON products(product_name);
CREATE INDEX idx_products_code ON products(product_code);
CREATE INDEX idx_products_active ON products(is_active);

CREATE INDEX idx_raw_materials_name ON raw_materials(material_name);
CREATE INDEX idx_raw_materials_code ON raw_materials(material_code);
CREATE INDEX idx_raw_materials_active ON raw_materials(is_active);

CREATE INDEX idx_recipes_product ON recipes(product_id);
CREATE INDEX idx_recipes_active ON recipes(is_active);

CREATE INDEX idx_recipe_ingredients_recipe ON recipe_ingredients(recipe_id);
CREATE INDEX idx_recipe_ingredients_material ON recipe_ingredients(raw_material_id);

CREATE INDEX idx_production_plans_date ON production_plans(plan_date);
CREATE INDEX idx_production_plans_status ON production_plans(status);

CREATE INDEX idx_production_plan_items_plan ON production_plan_items(production_plan_id);

CREATE INDEX idx_production_orders_plan ON production_orders(production_plan_id);
CREATE INDEX idx_production_orders_number ON production_orders(order_number);
CREATE INDEX idx_production_orders_status ON production_orders(status);

CREATE INDEX idx_production_order_items_order ON production_order_items(production_order_id);

CREATE INDEX idx_raw_material_requirements_order ON raw_material_requirements(production_order_id);
CREATE INDEX idx_raw_material_requirements_material ON raw_material_requirements(raw_material_id);
