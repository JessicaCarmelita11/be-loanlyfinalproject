-- =============================================
-- V3: Tier-Based Interest Rate Schema Update
-- =============================================
-- Run this script in SQL Server Management Studio (SSMS)
-- Or run each section separately if not using SSMS

-- Step 1: Delete existing tenor_rates data first
DELETE FROM tenor_rates;

-- Step 2: Drop the unique constraint on tenor_month (name may vary)
-- Check your constraint name in database first
-- Common names: UQ__tenor_ra__..., UK_tenor_rates_tenor_month, etc.
-- Run this query to find constraint name:
-- SELECT name FROM sys.indexes WHERE object_id = OBJECT_ID('tenor_rates') AND is_unique_constraint = 1;

-- Try dropping if exists (uncomment and modify as needed):
-- ALTER TABLE tenor_rates DROP CONSTRAINT [your_constraint_name_here];

-- Step 3: Add plafond_id column
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('tenor_rates') AND name = 'plafond_id')
BEGIN
    ALTER TABLE tenor_rates ADD plafond_id BIGINT NULL;
END

-- Step 4: Now make it NOT NULL (data already deleted above)
ALTER TABLE tenor_rates ALTER COLUMN plafond_id BIGINT NOT NULL;

-- Step 5: Add foreign key constraint
IF NOT EXISTS (SELECT * FROM sys.foreign_keys WHERE name = 'FK_tenor_rates_plafond')
BEGIN
    ALTER TABLE tenor_rates ADD CONSTRAINT FK_tenor_rates_plafond 
        FOREIGN KEY (plafond_id) REFERENCES plafonds(id);
END

-- Step 6: Add composite unique constraint
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'UQ_plafond_tenor')
BEGIN
    ALTER TABLE tenor_rates ADD CONSTRAINT UQ_plafond_tenor 
        UNIQUE (plafond_id, tenor_month);
END

PRINT 'V3 Schema update completed: tenor_rates now supports tier-based interest rates';
