-- =====================================================
-- V5: Add Bank Name Field
-- For Plafond Applications (UserPlafond)
-- =====================================================

-- Add bank_name column to user_plafonds
ALTER TABLE user_plafonds ADD bank_name VARCHAR(50) NULL;

-- Optional: Add comments for documentation
-- EXEC sp_addextendedproperty 'MS_Description', 
--     'Name of the bank where the account number belongs to', 
--     'SCHEMA', 'dbo', 'TABLE', 'user_plafonds', 'COLUMN', 'bank_name';
