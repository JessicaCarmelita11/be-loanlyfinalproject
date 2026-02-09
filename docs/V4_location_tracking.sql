-- =====================================================
-- V4: Add Location Tracking Columns
-- For Plafond Applications and Disbursement Requests
-- =====================================================

-- Add location columns to user_plafonds (for plafond application location)
ALTER TABLE user_plafonds ADD application_latitude DECIMAL(10, 7) NULL;
ALTER TABLE user_plafonds ADD application_longitude DECIMAL(10, 7) NULL;

-- Add location columns to disbursements (for disbursement request location)
ALTER TABLE disbursements ADD request_latitude DECIMAL(10, 7) NULL;
ALTER TABLE disbursements ADD request_longitude DECIMAL(10, 7) NULL;

-- Optional: Add comments for documentation
-- EXEC sp_addextendedproperty 'MS_Description', 
--     'Latitude where the plafond application was submitted from Android device', 
--     'SCHEMA', 'dbo', 'TABLE', 'user_plafonds', 'COLUMN', 'application_latitude';
