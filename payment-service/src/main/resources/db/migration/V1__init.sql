-- V1__init.sql  (payment-service)
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

CREATE TABLE IF NOT EXISTS bank_accounts (
    id uuid NOT NULL,
    account_last_four character varying(4),
    balance numeric(12,2) NOT NULL,
    bank_name character varying(100) NOT NULL,
    created_at timestamp(6) without time zone,
    razorpay_token_id character varying(50),
    user_id character varying(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS scheduled_bills (
    id uuid NOT NULL,
    amount numeric(12,2) NOT NULL,
    created_at timestamp(6) without time zone,
    due_date date NOT NULL,
    payee_name character varying(120),
    status character varying(20),
    type character varying(20) NOT NULL,
    user_id character varying(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS tokenized_accounts (
    id uuid NOT NULL,
    account_last_four character varying(4),
    bank_name character varying(100),
    created_at timestamp(6) without time zone,
    ifsc_code character varying(11),
    razorpay_customer_id character varying(255) NOT NULL,
    razorpay_token_id character varying(255) NOT NULL,
    updated_at timestamp(6) without time zone,
    user_id character varying(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS tokenized_cards (
    id uuid NOT NULL,
    card_last_four character varying(4),
    card_network character varying(20),
    created_at timestamp(6) without time zone,
    razorpay_customer_id character varying(255) NOT NULL,
    razorpay_token_id character varying(255) NOT NULL,
    updated_at timestamp(6) without time zone,
    user_id character varying(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS tokenized_upi_ids (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    masked_upi character varying(50),
    razorpay_customer_id character varying(255) NOT NULL,
    razorpay_token_id character varying(255) NOT NULL,
    updated_at timestamp(6) without time zone,
    user_id character varying(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS transactions (
    id uuid NOT NULL,
    amount numeric(12,2) NOT NULL,
    created_at timestamp(6) without time zone,
    currency character varying(3),
    payee_upi character varying(255),
    razorpay_order_id character varying(100),
    razorpay_payment_id character varying(100),
    status character varying(20),
    type character varying(20) NOT NULL,
    user_id character varying(255) NOT NULL
);

DO $$ BEGIN
    ALTER TABLE bank_accounts ADD CONSTRAINT bank_accounts_pkey PRIMARY KEY (id);
EXCEPTION WHEN duplicate_table OR duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    ALTER TABLE scheduled_bills ADD CONSTRAINT scheduled_bills_pkey PRIMARY KEY (id);
EXCEPTION WHEN duplicate_table OR duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    ALTER TABLE tokenized_accounts ADD CONSTRAINT tokenized_accounts_pkey PRIMARY KEY (id);
EXCEPTION WHEN duplicate_table OR duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    ALTER TABLE tokenized_cards ADD CONSTRAINT tokenized_cards_pkey PRIMARY KEY (id);
EXCEPTION WHEN duplicate_table OR duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    ALTER TABLE tokenized_upi_ids ADD CONSTRAINT tokenized_upi_ids_pkey PRIMARY KEY (id);
EXCEPTION WHEN duplicate_table OR duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    ALTER TABLE transactions ADD CONSTRAINT transactions_pkey PRIMARY KEY (id);
EXCEPTION WHEN duplicate_table OR duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    ALTER TABLE tokenized_accounts ADD CONSTRAINT uk_9mj00lur3st2mc581foqpeuu1 UNIQUE (razorpay_token_id);
EXCEPTION WHEN duplicate_table OR duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    ALTER TABLE tokenized_cards ADD CONSTRAINT uk_a24w819u41pqc4hsuye4sefv6 UNIQUE (razorpay_token_id);
EXCEPTION WHEN duplicate_table OR duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    ALTER TABLE tokenized_upi_ids ADD CONSTRAINT uk_jaql7yt7fh5q206bu64gr6fga UNIQUE (razorpay_token_id);
EXCEPTION WHEN duplicate_table OR duplicate_object THEN NULL;
END $$;
