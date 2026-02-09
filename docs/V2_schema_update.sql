-- ============================================================
-- SQL SCRIPT: Schema Updates for Push Notifications & Account Details
-- ============================================================

-- 1. Add fcm_token column to users table
IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS 
               WHERE TABLE_NAME = 'users' AND COLUMN_NAME = 'fcm_token')
BEGIN
    ALTER TABLE users ADD fcm_token VARCHAR(500) NULL;
    PRINT 'Added fcm_token column to users table';
END
ELSE
BEGIN
    PRINT 'Column fcm_token already exists in users table';
END

-- 2. Add account_number column to user_plafonds table
IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS 
               WHERE TABLE_NAME = 'user_plafonds' AND COLUMN_NAME = 'account_number')
BEGIN
    ALTER TABLE user_plafonds ADD account_number VARCHAR(50) NULL;
    PRINT 'Added account_number column to user_plafonds table';
END
ELSE
BEGIN
    PRINT 'Column account_number already exists in user_plafonds table';
END

-- 3. Verify changes
SELECT COLUMN_NAME, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_NAME = 'users' AND COLUMN_NAME = 'fcm_token';

SELECT COLUMN_NAME, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_NAME = 'user_plafonds' AND COLUMN_NAME = 'account_number';
