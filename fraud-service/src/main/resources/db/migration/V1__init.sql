-- V1__init.sql  (fraud-service)
--
-- Baseline schema, generated from the JPA entities by letting Hibernate build
-- the tables once and dumping the result with pg_dump. It is therefore exactly
-- what hibernate ddl-auto=validate expects; hand-written DDL would drift.
--
-- This replaces ddl-auto=update, which silently reshaped production tables on
-- every boot with no record of what changed.
--
-- NOTE: payment-service and fraud-service share one database. Each keeps its
-- own Flyway history table (flyway_schema_history_payment / _fraud) so their
-- migration records do not overwrite one another.

CREATE TABLE IF NOT EXISTS budgets (
    id uuid NOT NULL,
    category character varying(100) NOT NULL,
    created_at timestamp(6) without time zone,
    current_spent numeric(12,2),
    month_year character varying(7) NOT NULL,
    monthly_limit numeric(12,2) NOT NULL,
    user_id character varying(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS fd_rates (
    id uuid NOT NULL,
    bank_name character varying(200) NOT NULL,
    general_rate double precision NOT NULL,
    last_updated timestamp(6) without time zone,
    max_amount numeric(12,2),
    min_amount numeric(12,2),
    senior_rate double precision,
    special_scheme character varying(200),
    tenure_months integer NOT NULL
);

CREATE TABLE IF NOT EXISTS fraud_patterns (
    id uuid NOT NULL,
    category character varying(50) NOT NULL,
    created_at timestamp(6) without time zone,
    keywords character varying(500),
    match_count integer,
    pattern_description character varying(2000) NOT NULL,
    severity character varying(20) NOT NULL
);

CREATE TABLE IF NOT EXISTS fraud_transaction_view (
    id uuid NOT NULL,
    amount numeric(12,2) NOT NULL,
    created_at timestamp(6) without time zone,
    currency character varying(3),
    payee_upi character varying(255),
    razorpay_order_id character varying(100),
    status character varying(20),
    type character varying(20) NOT NULL,
    user_id character varying(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS scam_reports (
    id uuid NOT NULL,
    ai_analysis text,
    created_at timestamp(6) without time zone,
    description text,
    reported_upi character varying(255),
    reporter_id character varying(255) NOT NULL,
    scam_type character varying(50),
    sms_content text
);

DO $$ BEGIN
    ALTER TABLE budgets ADD CONSTRAINT budgets_pkey PRIMARY KEY (id);
EXCEPTION WHEN duplicate_table OR duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    ALTER TABLE fd_rates ADD CONSTRAINT fd_rates_pkey PRIMARY KEY (id);
EXCEPTION WHEN duplicate_table OR duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    ALTER TABLE fraud_patterns ADD CONSTRAINT fraud_patterns_pkey PRIMARY KEY (id);
EXCEPTION WHEN duplicate_table OR duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    ALTER TABLE fraud_transaction_view ADD CONSTRAINT fraud_transaction_view_pkey PRIMARY KEY (id);
EXCEPTION WHEN duplicate_table OR duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    ALTER TABLE scam_reports ADD CONSTRAINT scam_reports_pkey PRIMARY KEY (id);
EXCEPTION WHEN duplicate_table OR duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    ALTER TABLE budgets ADD CONSTRAINT uk_budget_user_category_month UNIQUE (user_id, category, month_year);
EXCEPTION WHEN duplicate_table OR duplicate_object THEN NULL;
END $$;
