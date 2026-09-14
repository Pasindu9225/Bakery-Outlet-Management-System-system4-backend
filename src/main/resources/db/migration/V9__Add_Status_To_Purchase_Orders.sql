-- DB Migration V9: Add status column to purchase_orders table
-- Ensure the status column exists for tracking PO lifecycle (PENDING, APPROVED, RECEIVED, etc.)
ALTER TABLE purchase_orders ADD COLUMN status VARCHAR(20) DEFAULT 'PENDING' NOT NULL;
