-- V2__backfill_fraud_transaction_view.sql  (fraud-service)
--
-- Carries existing rows over after the read model was moved off the shared
-- "transactions" table.
--
-- Background: fraud-service used to map its Transaction entity to
-- "transactions" - the same table payment-service owns, but with a different
-- column set (payment has razorpay_payment_id, fraud does not). Both ran
-- ddl-auto=update against one database, so whichever service booted last
-- reshaped the table. fraud-service now owns fraud_transaction_view instead.
--
-- Without this backfill, an upgraded deployment would start with an empty
-- projection: the Kafka consumer group (safepe-ai-readmodel-group) resumes
-- from its committed offsets rather than replaying history, so Spending
-- Insights and the Money Assistant would report no transactions until new
-- payments arrived.
--
-- Guarded three ways so it is safe on a fresh database, safe to re-run, and
-- safe when payment-service has not migrated yet:
--   1. skip entirely if "transactions" does not exist
--   2. skip if the view already holds rows
--   3. ON CONFLICT DO NOTHING on the primary key

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'public' AND table_name = 'transactions'
    ) THEN
        RAISE NOTICE 'No legacy transactions table; nothing to backfill.';
        RETURN;
    END IF;

    IF EXISTS (SELECT 1 FROM fraud_transaction_view LIMIT 1) THEN
        RAISE NOTICE 'fraud_transaction_view already populated; skipping backfill.';
        RETURN;
    END IF;

    INSERT INTO fraud_transaction_view (
        id, user_id, payee_upi, amount, currency, type, status,
        razorpay_order_id, created_at
    )
    SELECT
        t.id,
        t.user_id,
        t.payee_upi,
        t.amount,
        COALESCE(t.currency, 'INR'),
        COALESCE(t.type, 'UPI'),
        COALESCE(t.status, 'SUCCESS'),
        t.razorpay_order_id,
        t.created_at
    FROM transactions t
    ON CONFLICT (id) DO NOTHING;

    RAISE NOTICE 'Backfilled % row(s) into fraud_transaction_view.',
        (SELECT count(*) FROM fraud_transaction_view);
END $$;
