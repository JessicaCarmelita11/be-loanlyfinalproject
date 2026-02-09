-- =====================================================
-- V6: Fix Interest Calculation Data
-- Recalculate interest_amount and total_amount for existing records
-- based on the new Monthly Fixed Rate formula:
-- Interest = Amount * (Rate/100) * Tenor
-- =====================================================

UPDATE disbursements
SET 
    interest_amount = amount * (interest_rate / 100.0) * tenor_month,
    total_amount = amount + (amount * (interest_rate / 100.0) * tenor_month);

-- Optional: Verify the changes
-- SELECT id, amount, tenor_month, interest_rate, interest_amount, total_amount FROM disbursements;
