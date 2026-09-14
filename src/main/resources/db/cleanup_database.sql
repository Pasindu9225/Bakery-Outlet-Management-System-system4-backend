-- =====================================================================
-- Bakery Management System - Complete Database Cleanup Script
-- Purpose: Remove all transactional, stock, BOM, sales, GRN, production,
--          finance, master catalog, and reference data while keeping ONLY
--          User authentication and Outlet infrastructure data.
-- WARNING: This script TRUNCATES database tables!
-- =====================================================================

SET FOREIGN_KEY_CHECKS = 0;

-- 1. Master Catalogs & Setup Data (Requested for removal)
TRUNCATE TABLE category;
TRUNCATE TABLE brands;
TRUNCATE TABLE products;
TRUNCATE TABLE pack_details;
TRUNCATE TABLE raw_materials;
TRUNCATE TABLE generic_materials;
TRUNCATE TABLE suppliers;
TRUNCATE TABLE raw_material_suppliers;
TRUNCATE TABLE customers;
TRUNCATE TABLE payment_methods;
TRUNCATE TABLE discounts;
TRUNCATE TABLE promotions;

-- 2. BOM & Recipe Data
TRUNCATE TABLE bill_of_materials;
TRUNCATE TABLE recipes;
TRUNCATE TABLE recipe_ingredients;

-- 3. Stock & Inventory Data
TRUNCATE TABLE mini_stores;
TRUNCATE TABLE mini_store_items;
TRUNCATE TABLE stock_adjust;
TRUNCATE TABLE raw_material_requirements;

-- 4. Procurement & Storekeeper Data
TRUNCATE TABLE purchase_orders;
TRUNCATE TABLE purchase_order_items;
TRUNCATE TABLE grn;
TRUNCATE TABLE grn_item;
TRUNCATE TABLE iou_requests;
TRUNCATE TABLE iou_request_items;
TRUNCATE TABLE raw_material_return;
TRUNCATE TABLE raw_material_return_items;

-- 5. Production & Planning Data
TRUNCATE TABLE production_plans;
TRUNCATE TABLE production_plan_items;
TRUNCATE TABLE production_plan_materials;
TRUNCATE TABLE production_orders;
TRUNCATE TABLE production_order_items;
TRUNCATE TABLE production_batches;
TRUNCATE TABLE production_stages;
TRUNCATE TABLE actual_productions;
TRUNCATE TABLE actual_production_history;
TRUNCATE TABLE day_production;
TRUNCATE TABLE day_production_items;
TRUNCATE TABLE ingredient_requests;
TRUNCATE TABLE ingredient_request_items;
TRUNCATE TABLE kitchen_returns;
TRUNCATE TABLE kitchen_return_items;

-- 6. Distribution & Transfer Data
TRUNCATE TABLE distributhio_plans;
TRUNCATE TABLE distribution_plan_items;
TRUNCATE TABLE transfer_notes;
TRUNCATE TABLE transfer_note_items;
TRUNCATE TABLE gtn;
TRUNCATE TABLE gtn_item;
TRUNCATE TABLE gtn_day_production;
TRUNCATE TABLE outlet_transfer_requests;
TRUNCATE TABLE outlet_returns;
TRUNCATE TABLE outlet_return_items;

-- 7. POS & Sales Transactions
TRUNCATE TABLE sales;
TRUNCATE TABLE sale_items;
TRUNCATE TABLE `returns`;
TRUNCATE TABLE return_items;
TRUNCATE TABLE special_orders;
TRUNCATE TABLE special_order_items;
TRUNCATE TABLE table_items;
TRUNCATE TABLE pos_waiter_items;
TRUNCATE TABLE cash_floats;
TRUNCATE TABLE shift_closures;
TRUNCATE TABLE day_end_closings;
TRUNCATE TABLE day_end_closing_items;
TRUNCATE TABLE discount_audit;
TRUNCATE TABLE customer_points_history;

-- 8. Finance & Accounting Transactions
TRUNCATE TABLE supplier_payments;
TRUNCATE TABLE supplier_payment_allocations;
TRUNCATE TABLE purchase_returns;
TRUNCATE TABLE purchase_return_items;
TRUNCATE TABLE manual_ledger_adjustments;

-- 9. Notifications & System Logs
TRUNCATE TABLE notifications;

SET FOREIGN_KEY_CHECKS = 1;
