-- Add waiter_id column to bmsauth table to support waiters without login
ALTER TABLE bmsauth ADD COLUMN waiter_id VARCHAR(50) UNIQUE;

-- Ensure username and password_hash can be NULL for users (waiters) who don't log in
-- Note: In some databases, columns might already be nullable. 
-- We explicitly set them to nullable just in case.
ALTER TABLE bmsauth MODIFY COLUMN username VARCHAR(255) NULL;
ALTER TABLE bmsauth MODIFY COLUMN password_hash VARCHAR(255) NULL;
