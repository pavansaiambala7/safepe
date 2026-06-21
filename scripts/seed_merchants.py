"""
SafePe — Merchant & UPI Fraud Database Seeder
================================================
This script generates 550+ realistic Indian merchants with:
- Weighted trust scores (0-100)
- Fake UPI IDs
- Fraud risk categories
- Business categories

Prerequisites:
    pip install faker psycopg2-binary

Usage:
    python scripts/seed_merchants.py
"""

import random
import uuid
from datetime import datetime, timedelta
from faker import Faker

# We use Indian locale for realistic Indian names and addresses
fake = Faker('en_IN')

# ============================================================================
# CONFIGURATION — Match these with your docker-compose.yml / .env
# ============================================================================
DB_CONFIG = {
    "host": "localhost",
    "port": 5432,
    "database": "safepe",
    "user": "postgres",
    "password": "postgres"
}

# ============================================================================
# MERCHANT CATEGORIES — Real Indian business types
# ============================================================================
MERCHANT_CATEGORIES = [
    "Grocery", "Restaurant", "Fuel Station", "Electronics",
    "Clothing", "Pharmacy", "Hospital", "Education",
    "Travel", "Entertainment", "Insurance", "Utilities",
    "E-Commerce", "Food Delivery", "Ride Sharing",
    "Mobile Recharge", "DTH Recharge", "Gold & Jewelry",
    "Real Estate", "Government Services"
]

# ============================================================================
# UPI HANDLE SUFFIXES — Real Indian UPI providers
# ============================================================================
UPI_SUFFIXES = [
    "@ybl", "@paytm", "@okaxis", "@oksbi", "@okhdfcbank",
    "@upi", "@ibl", "@axl", "@sbi", "@icici",
    "@kotak", "@boi", "@pnb", "@unionbank", "@indus"
]

# ============================================================================
# SCAM PATTERNS — Known fraud UPI patterns for training
# ============================================================================
SCAM_UPI_PATTERNS = [
    "customercare", "refund", "cashback", "lottery",
    "prize", "winner", "kycupdate", "verification",
    "support", "helpdesk", "claim", "bonus",
    "offer2024", "freecharge", "luckywin"
]

SCAM_MERCHANT_NAMES = [
    "KYC Update Center", "Lottery Winner Desk", "Cashback Refund Portal",
    "Customer Care Executive", "Prize Claim Office", "Free Recharge Center",
    "Government Subsidy Desk", "Insurance Claim Help", "Tax Refund Portal",
    "Lucky Draw Winner", "Online Survey Reward", "Instant Loan Approval"
]


def generate_trust_score(is_fraudulent: bool) -> float:
    """
    Generate a weighted trust score.

    Legitimate merchants: Score between 60-100 (mostly 75-95)
    Fraudulent merchants: Score between 0-35 (mostly 5-20)

    We use a weighted random distribution so scores cluster
    around realistic values instead of being uniformly random.
    """
    if is_fraudulent:
        # Fraud merchants get very low scores
        # Weight: 70% chance of 0-15, 30% chance of 15-35
        if random.random() < 0.7:
            return round(random.uniform(0, 15), 2)
        else:
            return round(random.uniform(15, 35), 2)
    else:
        # Legitimate merchants get high scores
        # Weight: 60% chance of 80-100, 30% chance of 65-80, 10% chance of 50-65
        roll = random.random()
        if roll < 0.6:
            return round(random.uniform(80, 100), 2)
        elif roll < 0.9:
            return round(random.uniform(65, 80), 2)
        else:
            return round(random.uniform(50, 65), 2)


def generate_upi_id(name: str, is_fraudulent: bool) -> str:
    """
    Generate a realistic UPI ID.

    Legitimate: Uses the merchant's name (e.g., "reliance.retail@okaxis")
    Fraudulent: Uses scam patterns (e.g., "customercare8392@ybl")
    """
    suffix = random.choice(UPI_SUFFIXES)

    if is_fraudulent:
        pattern = random.choice(SCAM_UPI_PATTERNS)
        random_digits = random.randint(1000, 9999)
        return f"{pattern}{random_digits}{suffix}"
    else:
        # Clean the name: lowercase, remove spaces, take first 15 chars
        clean_name = name.lower().replace(" ", ".").replace("'", "")[:15]
        random_digits = random.randint(10, 99)
        return f"{clean_name}{random_digits}{suffix}"


def generate_legitimate_merchant() -> dict:
    """Generate a single legitimate (safe) merchant."""
    name = fake.company()
    category = random.choice(MERCHANT_CATEGORIES)
    upi_id = generate_upi_id(name, is_fraudulent=False)
    trust_score = generate_trust_score(is_fraudulent=False)
    total_transactions = random.randint(100, 50000)
    reported_frauds = random.randint(0, int(total_transactions * 0.02))

    return {
        "id": str(uuid.uuid4()),
        "merchant_name": name,
        "upi_id": upi_id,
        "category": category,
        "trust_score": trust_score,
        "is_verified": random.random() < 0.85,  # 85% are verified
        "total_transactions": total_transactions,
        "reported_frauds": reported_frauds,
        "risk_level": "LOW",
        "registered_date": fake.date_between(
            start_date='-3y', end_date='today'
        ).isoformat(),
        "city": fake.city(),
        "state": fake.state(),
        "is_fraudulent": False
    }


def generate_fraudulent_merchant() -> dict:
    """Generate a single fraudulent (scam) merchant."""
    # 50% chance of using a known scam name, 50% chance of a fake company name
    if random.random() < 0.5:
        name = random.choice(SCAM_MERCHANT_NAMES)
    else:
        name = fake.company()

    category = random.choice(MERCHANT_CATEGORIES)
    upi_id = generate_upi_id(name, is_fraudulent=True)
    trust_score = generate_trust_score(is_fraudulent=True)
    total_transactions = random.randint(1, 500)  # Scammers have fewer transactions
    reported_frauds = random.randint(
        int(total_transactions * 0.3),
        total_transactions
    )

    return {
        "id": str(uuid.uuid4()),
        "merchant_name": name,
        "upi_id": upi_id,
        "category": category,
        "trust_score": trust_score,
        "is_verified": random.random() < 0.1,  # Only 10% falsely verified
        "total_transactions": total_transactions,
        "reported_frauds": reported_frauds,
        "risk_level": random.choice(["HIGH", "CRITICAL"]),
        "registered_date": fake.date_between(
            start_date='-6m', end_date='today'  # Scammers are recent
        ).isoformat(),
        "city": fake.city(),
        "state": fake.state(),
        "is_fraudulent": True
    }


def seed_to_database(merchants: list):
    """
    Insert all merchants into the PostgreSQL database.
    Requires: pip install psycopg2-binary
    """
    try:
        import psycopg2

        conn = psycopg2.connect(**DB_CONFIG)
        cursor = conn.cursor()

        # Create the merchants table if it doesn't exist
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS merchants (
                id UUID PRIMARY KEY,
                merchant_name VARCHAR(255) NOT NULL,
                upi_id VARCHAR(100) UNIQUE NOT NULL,
                category VARCHAR(50),
                trust_score DOUBLE PRECISION DEFAULT 50.0,
                is_verified BOOLEAN DEFAULT FALSE,
                total_transactions INTEGER DEFAULT 0,
                reported_frauds INTEGER DEFAULT 0,
                risk_level VARCHAR(20) DEFAULT 'MEDIUM',
                registered_date DATE,
                city VARCHAR(100),
                state VARCHAR(100),
                is_fraudulent BOOLEAN DEFAULT FALSE
            );
        """)

        # Insert each merchant
        insert_query = """
            INSERT INTO merchants (
                id, merchant_name, upi_id, category, trust_score,
                is_verified, total_transactions, reported_frauds,
                risk_level, registered_date, city, state, is_fraudulent
            ) VALUES (
                %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s
            )
            ON CONFLICT (upi_id) DO NOTHING;
        """

        inserted = 0
        for m in merchants:
            cursor.execute(insert_query, (
                m["id"], m["merchant_name"], m["upi_id"], m["category"],
                m["trust_score"], m["is_verified"], m["total_transactions"],
                m["reported_frauds"], m["risk_level"], m["registered_date"],
                m["city"], m["state"], m["is_fraudulent"]
            ))
            inserted += 1

        conn.commit()
        cursor.close()
        conn.close()

        print(f"\n✅ Successfully inserted {inserted} merchants into PostgreSQL!")
        return True

    except ImportError:
        print("\n❌ psycopg2 not installed. Run: pip install psycopg2-binary")
        return False
    except Exception as e:
        print(f"\n❌ Database error: {e}")
        print("   Make sure Docker is running: docker-compose up -d")
        return False


def seed_to_json(merchants: list):
    """
    Save merchants to a JSON file (backup / alternative to database).
    """
    import json
    output_path = "scripts/merchants_data.json"
    with open(output_path, "w", encoding="utf-8") as f:
        json.dump(merchants, f, indent=2, ensure_ascii=False)
    print(f"✅ Saved {len(merchants)} merchants to {output_path}")


def main():
    print("=" * 60)
    print("  SafePe — Merchant & UPI Fraud Database Seeder")
    print("=" * 60)

    # ── Generate Merchants ──
    NUM_LEGITIMATE = 450   # 450 safe merchants
    NUM_FRAUDULENT = 100   # 100 scam merchants
    # Total: 550 merchants

    print(f"\n🏪 Generating {NUM_LEGITIMATE} legitimate merchants...")
    legitimate = [generate_legitimate_merchant() for _ in range(NUM_LEGITIMATE)]

    print(f"🚨 Generating {NUM_FRAUDULENT} fraudulent merchants...")
    fraudulent = [generate_fraudulent_merchant() for _ in range(NUM_FRAUDULENT)]

    all_merchants = legitimate + fraudulent
    random.shuffle(all_merchants)  # Mix them up

    # ── Print Summary ──
    avg_legit_score = sum(m["trust_score"] for m in legitimate) / len(legitimate)
    avg_fraud_score = sum(m["trust_score"] for m in fraudulent) / len(fraudulent)

    print(f"\n📊 Summary:")
    print(f"   Total merchants: {len(all_merchants)}")
    print(f"   Legitimate: {NUM_LEGITIMATE} (avg trust: {avg_legit_score:.1f})")
    print(f"   Fraudulent: {NUM_FRAUDULENT} (avg trust: {avg_fraud_score:.1f})")

    # ── Save to JSON (always works, no database needed) ──
    seed_to_json(all_merchants)

    # ── Try to insert into PostgreSQL ──
    print(f"\n🗄️  Attempting to insert into PostgreSQL...")
    seed_to_database(all_merchants)

    print(f"\n✅ Seeding complete!")
    print(f"   You can now query merchants in your Spring Boot app.")


if __name__ == "__main__":
    main()
