-- ============================================================
-- SQL SCRIPT: Complete Loan Workflow Simulation
-- Flow: Customer Apply -> Marketing Review -> Branch Manager Approve 
--       -> Customer Request Disbursement -> Back Office Disburse
-- ============================================================

-- ============================================================
-- PREREQUISITE: Make sure plafonds and tenor_rates exist
-- ============================================================

-- Insert plafonds if not exists
IF NOT EXISTS (SELECT 1 FROM plafonds WHERE name = 'Gold')
BEGIN
    INSERT INTO plafonds (name, max_amount, description, is_active, created_at) VALUES
    ('Plus', 5000000.00, 'Plafond Plus dengan limit maksimal Rp 5.000.000', 1, GETDATE()),
    ('Bronze', 15000000.00, 'Plafond Bronze dengan limit maksimal Rp 15.000.000', 1, GETDATE()),
    ('Silver', 25000000.00, 'Plafond Silver dengan limit maksimal Rp 25.000.000', 1, GETDATE()),
    ('Gold', 50000000.00, 'Plafond Gold dengan limit maksimal Rp 50.000.000', 1, GETDATE()),
    ('Diamond', 100000000.00, 'Plafond Diamond dengan limit maksimal Rp 100.000.000', 1, GETDATE()),
    ('VVIP', 200000000.00, 'Plafond VVIP dengan limit maksimal Rp 200.000.000', 1, GETDATE());
END

-- ============================================================
-- STEP 0: Get User IDs (Run this first to get IDs)
-- ============================================================
-- Check existing users
SELECT id, username, full_name FROM users;
-- Check existing plafonds
SELECT id, name, max_amount FROM plafonds WHERE deleted_at IS NULL;
-- Check tenor rates
SELECT id, tenor_month, interest_rate FROM tenor_rates WHERE is_active = 1;

-- ============================================================
-- STEP 1: Customer Applies for Plafond (PENDING_REVIEW)
-- Replace @customer_id and @plafond_id with actual IDs
-- ============================================================

DECLARE @customer_id BIGINT = 6;  -- ID of customer user (e.g., 'joko')
DECLARE @plafond_id BIGINT = 4;   -- ID of plafond (e.g., 'Gold')
DECLARE @user_plafond_id BIGINT;

-- Insert application
INSERT INTO user_plafonds (
    user_id, 
    plafond_id, 
    status, 
    registered_at,
    used_amount,
    -- Applicant details
    nik,
    birth_place,
    birth_date,
    marital_status,
    occupation,
    monthly_income,
    phone,
    npwp,
    account_number
) VALUES (
    @customer_id,
    @plafond_id,
    'PENDING_REVIEW',
    GETDATE(),
    0,
    -- Applicant details
    '3201234567890001',
    'Jakarta',
    '1990-05-15',
    'Single',
    'Software Engineer',
    15000000.00,
    '081234567890',
    '12.345.678.9-012.000',
    '1234567890' -- account_number
);

SET @user_plafond_id = SCOPE_IDENTITY();
PRINT 'Created user_plafond with ID: ' + CAST(@user_plafond_id AS VARCHAR);

-- Insert history for application
INSERT INTO plafond_histories (
    user_plafond_id,
    previous_status,
    new_status,
    action_by_user_id,
    action_by_role,
    note,
    created_at
) VALUES (
    @user_plafond_id,
    'PENDING_REVIEW',
    'PENDING_REVIEW',
    @customer_id,
    'CUSTOMER',
    'Application submitted',
    GETDATE()
);

-- Create notification for customer
INSERT INTO notifications (
    user_id,
    title,
    message,
    type,
    is_read,
    reference_id,
    created_at
) VALUES (
    @customer_id,
    'Pengajuan Limit Kredit Diterima',
    'Pengajuan limit kredit Anda untuk Gold sedang dalam proses review.',
    'LOAN_SUBMITTED',
    0,
    @user_plafond_id,
    GETDATE()
);

PRINT '=== STEP 1 COMPLETE: Customer application submitted ===';

-- ============================================================
-- STEP 2: Marketing Reviews and Approves (WAITING_APPROVAL)
-- ============================================================

DECLARE @marketing_id BIGINT = 2;  -- ID of marketing user

-- Update status to WAITING_APPROVAL
UPDATE user_plafonds 
SET 
    status = 'WAITING_APPROVAL',
    reviewed_by = @marketing_id,
    reviewed_at = GETDATE()
WHERE id = @user_plafond_id;

-- Insert history for review
INSERT INTO plafond_histories (
    user_plafond_id,
    previous_status,
    new_status,
    action_by_user_id,
    action_by_role,
    note,
    created_at
) VALUES (
    @user_plafond_id,
    'PENDING_REVIEW',
    'WAITING_APPROVAL',
    @marketing_id,
    'MARKETING',
    'Documents verified, forwarding to Branch Manager',
    GETDATE()
);

-- Create notification for customer
INSERT INTO notifications (
    user_id,
    title,
    message,
    type,
    is_read,
    reference_id,
    created_at
) VALUES (
    @customer_id,
    'Pengajuan Disetujui Marketing',
    'Pengajuan limit kredit Anda telah diverifikasi dan menunggu persetujuan final.',
    'LOAN_REVIEWED',
    0,
    @user_plafond_id,
    GETDATE()
);

PRINT '=== STEP 2 COMPLETE: Marketing reviewed ===';

-- ============================================================
-- STEP 3: Branch Manager Approves (APPROVED)
-- ============================================================

DECLARE @branch_manager_id BIGINT = 3;  -- ID of branch manager
DECLARE @approved_limit DECIMAL(18,2) = 40000000.00;  -- Limit yang disetujui

-- Update status to APPROVED
UPDATE user_plafonds 
SET 
    status = 'APPROVED',
    approved_by = @branch_manager_id,
    approved_at = GETDATE(),
    approved_limit = @approved_limit,
    used_amount = 0
WHERE id = @user_plafond_id;

-- Insert history for approval
INSERT INTO plafond_histories (
    user_plafond_id,
    previous_status,
    new_status,
    action_by_user_id,
    action_by_role,
    note,
    created_at
) VALUES (
    @user_plafond_id,
    'WAITING_APPROVAL',
    'APPROVED',
    @branch_manager_id,
    'BRANCH_MANAGER',
    'Approved with limit Rp 40.000.000',
    GETDATE()
);

-- Create notification for customer
INSERT INTO notifications (
    user_id,
    title,
    message,
    type,
    is_read,
    reference_id,
    created_at
) VALUES (
    @customer_id,
    'Limit Kredit Disetujui!',
    'Selamat! Anda mendapat limit kredit sebesar Rp 40.000.000. Anda dapat melakukan pencairan kapan saja.',
    'LOAN_APPROVED',
    0,
    @user_plafond_id,
    GETDATE()
);

PRINT '=== STEP 3 COMPLETE: Branch Manager approved ===';

-- ============================================================
-- STEP 4: Customer Requests Disbursement (PENDING)
-- ============================================================

DECLARE @disbursement_amount DECIMAL(18,2) = 10000000.00;  -- Rp 10 juta
DECLARE @tenor_month INT = 12;  -- 12 bulan
DECLARE @interest_rate DECIMAL(5,2);
DECLARE @interest_amount DECIMAL(18,2);
DECLARE @total_amount DECIMAL(18,2);
DECLARE @disbursement_id BIGINT;

-- Get interest rate from tenor_rates
SELECT @interest_rate = interest_rate 
FROM tenor_rates 
WHERE tenor_month = @tenor_month AND is_active = 1;

-- Calculate interest: amount × (rate/100) × (tenor/12)
SET @interest_amount = @disbursement_amount * (@interest_rate / 100) * (@tenor_month / 12.0);
SET @total_amount = @disbursement_amount + @interest_amount;

PRINT 'Disbursement Amount: ' + CAST(@disbursement_amount AS VARCHAR);
PRINT 'Interest Rate: ' + CAST(@interest_rate AS VARCHAR) + '%';
PRINT 'Tenor: ' + CAST(@tenor_month AS VARCHAR) + ' months';
PRINT 'Interest Amount: ' + CAST(@interest_amount AS VARCHAR);
PRINT 'Total Amount: ' + CAST(@total_amount AS VARCHAR);

-- Insert disbursement request
INSERT INTO disbursements (
    user_plafond_id,
    amount,
    interest_rate,
    tenor_month,
    interest_amount,
    total_amount,
    status,
    requested_at
) VALUES (
    @user_plafond_id,
    @disbursement_amount,
    @interest_rate,
    @tenor_month,
    @interest_amount,
    @total_amount,
    'PENDING',
    GETDATE()
);

SET @disbursement_id = SCOPE_IDENTITY();

-- Update used_amount on user_plafond (reserve the limit)
UPDATE user_plafonds 
SET used_amount = used_amount + @disbursement_amount
WHERE id = @user_plafond_id;

-- Create notification for customer
INSERT INTO notifications (
    user_id,
    title,
    message,
    type,
    is_read,
    reference_id,
    created_at
) VALUES (
    @customer_id,
    'Permintaan Pencairan Dikirim',
    'Pencairan sebesar Rp 10.000.000 dengan tenor 12 bulan sedang diproses. Total yang harus dibayar: Rp ' + CAST(@total_amount AS VARCHAR),
    'LOAN_SUBMITTED',
    0,
    @disbursement_id,
    GETDATE()
);

PRINT '=== STEP 4 COMPLETE: Customer requested disbursement ===';

-- ============================================================
-- STEP 5: Back Office Disburses (DISBURSED)
-- ============================================================

DECLARE @backoffice_id BIGINT = 4;  -- ID of back office user

-- Update disbursement status to DISBURSED
UPDATE disbursements 
SET 
    status = 'DISBURSED',
    disbursed_at = GETDATE(),
    disbursed_by = @backoffice_id,
    note = 'Dana telah ditransfer ke rekening customer'
WHERE id = @disbursement_id;

-- Create notification for customer
INSERT INTO notifications (
    user_id,
    title,
    message,
    type,
    is_read,
    reference_id,
    created_at
) VALUES (
    @customer_id,
    'Dana Telah Dicairkan!',
    'Pencairan sebesar Rp 10.000.000 telah berhasil diproses. Total yang harus dibayar: Rp ' + CAST(@total_amount AS VARCHAR),
    'LOAN_DISBURSED',
    0,
    @disbursement_id,
    GETDATE()
);

PRINT '=== STEP 5 COMPLETE: Back Office disbursed ===';

-- ============================================================
-- VERIFICATION: Check the results
-- ============================================================

PRINT '';
PRINT '=== VERIFICATION ===';

-- Check user_plafond
SELECT 
    up.id,
    u.username as customer,
    p.name as plafond,
    up.status,
    up.approved_limit,
    up.used_amount,
    (up.approved_limit - up.used_amount) as available_limit,
    reviewer.username as reviewed_by,
    approver.username as approved_by
FROM user_plafonds up
JOIN users u ON up.user_id = u.id
JOIN plafonds p ON up.plafond_id = p.id
LEFT JOIN users reviewer ON up.reviewed_by = reviewer.id
LEFT JOIN users approver ON up.approved_by = approver.id
WHERE up.id = @user_plafond_id;

-- Check disbursement
SELECT 
    d.id,
    d.amount,
    d.interest_rate,
    d.tenor_month,
    d.interest_amount,
    d.total_amount,
    d.status,
    d.requested_at,
    d.disbursed_at,
    bo.username as disbursed_by
FROM disbursements d
LEFT JOIN users bo ON d.disbursed_by = bo.id
WHERE d.id = @disbursement_id;

-- Check history
SELECT 
    ph.previous_status,
    ph.new_status,
    u.username as action_by,
    ph.action_by_role,
    ph.note,
    ph.created_at
FROM plafond_histories ph
JOIN users u ON ph.action_by_user_id = u.id
WHERE ph.user_plafond_id = @user_plafond_id
ORDER BY ph.created_at;

-- Check notifications
SELECT 
    n.title,
    n.message,
    n.type,
    n.is_read,
    n.created_at
FROM notifications n
WHERE n.user_id = @customer_id
ORDER BY n.created_at DESC;

