-- Allow SUBMITTED status for production plans
ALTER TABLE production_plans
    MODIFY COLUMN status ENUM('DRAFT', 'SUBMITTED', 'APPROVED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')
        NOT NULL DEFAULT 'DRAFT';


