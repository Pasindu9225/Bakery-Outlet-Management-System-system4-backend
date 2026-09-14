-- Fix invalid date values in distribution_plans table
-- This migration addresses the "Zero date value prohibited" error

-- Step 1: Update NULL dates to current date
UPDATE distributhio_plans 
SET date = CURDATE() 
WHERE date IS NULL;

-- Step 2: Update invalid '0000-00-00' dates to current date
-- Using a more robust approach that works across MySQL versions
UPDATE distributhio_plans 
SET date = CURDATE() 
WHERE date = '0000-00-00' 
   OR date = '0000-00-00 00:00:00'
   OR date < '1900-01-01';

-- Step 3: Make the date column NOT NULL with a default value
-- This prevents future NULL dates
ALTER TABLE distributhio_plans 
MODIFY COLUMN date DATE NOT NULL DEFAULT (CURDATE());

-- Step 4: Add an index on the date column for better performance
CREATE INDEX IF NOT EXISTS idx_distribution_plans_date ON distributhio_plans(date);

-- Step 5: Verify the fix by checking for any remaining invalid dates
-- This query should return 0 rows if the fix was successful
SELECT COUNT(*) as invalid_dates_count 
FROM distributhio_plans 
WHERE date IS NULL 
   OR date = '0000-00-00' 
   OR date < '1900-01-01';
