-- Database Migration for Customer Management and Loyalty Program

-- Create customers table if it doesn't exist (with decimal points and id_card_number)
CREATE TABLE IF NOT EXISTS customers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    contact_number VARCHAR(50) NOT NULL UNIQUE,
    id_card_number VARCHAR(50) NULL,
    email VARCHAR(255) NULL,
    address VARCHAR(255) NULL,
    loyalty_points DECIMAL(10, 3) NOT NULL DEFAULT 0.000,
    is_credit_allowed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Safely alter column if the table already existed with INT loyalty_points
ALTER TABLE customers MODIFY COLUMN loyalty_points DECIMAL(10, 3) NOT NULL DEFAULT 0.000;

-- Safely add id_card_number if the table already existed but lacked it
-- Check if column exists is handled by standard schema update or simple ALTER in dev databases
-- For standard MySQL:
ALTER TABLE customers ADD COLUMN IF NOT EXISTS id_card_number VARCHAR(50) NULL;

-- Create customer_points_history table
CREATE TABLE IF NOT EXISTS customer_points_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    sale_id INT NULL,
    points_changed DECIMAL(10, 3) NOT NULL,
    description VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cph_customer FOREIGN KEY (customer_id) REFERENCES customers(id),
    CONSTRAINT fk_cph_sale FOREIGN KEY (sale_id) REFERENCES sales(sale_id)
);

-- Add customer_id to sales table
ALTER TABLE sales ADD COLUMN IF NOT EXISTS customer_id BIGINT NULL;
ALTER TABLE sales ADD CONSTRAINT fk_sales_customer FOREIGN KEY (customer_id) REFERENCES customers(id);
