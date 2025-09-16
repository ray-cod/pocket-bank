# Pocket Bank — CLI Banking Simulator

**Pocket Bank** is a polished, production-minded command-line ATM / personal banking simulator written in **Java**.
It demonstrates secure authentication, clean layered architecture (repositories → services → CLI), explicit JDBC transactions (H2), and a professional CLI UX.

---

## Demo Screenshots



---

## Table of contents

* [Overview](#overview)
* [Key features](#key-features)
* [Tech stack](#tech-stack)
* [Requirements](#requirements)
* [Build & run](#build--run)
* [CLI quick reference & sample session](#cli-quick-reference--sample-session)
* [Database schema (concise)](#database-schema-concise)
* [Design & security notes](#design--security-notes)
* [Project structure (high level)](#project-structure-high-level)
* [Future improvements](#future-improvements)
* [License & contact](#license--contact)

---

## Overview

Pocket Bank is an interactive terminal application that simulates an ATM / personal banking flow:

* User authentication (username + masked PIN), lockout on repeated failures.
* Account management (multiple accounts per user).
* Core banking operations: **Deposit**, **Withdraw**, **Transfer** (atomic).
* Transaction persistence (H2) with full audit fields (`transaction_id`, `timestamp`, `balance_after`, `description`, optional `dest_account_id`).
* Polished CLI UX: masked PIN input, confirmations, pagination, CSV export.

> This project is a realistic learning/demo system, not intended for production banking.

---

## Key features

* Secure authentication: hashed PINs (use BCrypt/PBKDF2 via `CryptoUtil`).
* Account creation and deterministic account-number generation.
* Deposit / Withdraw / Transfer (transfers are atomic).
* Transaction history, pagination, and CSV export.
* Admin utilities: seed demo data, unlock users, and view all transactions.
* Clean separation: models → repositories → services → CLI UI.

---

## Tech stack

* Java 22: works with Java 17+
* H2 Database (embedded, in-memory or file)
* JDBC (explicit SQL & transactions)
* BCrypt (via `CryptoUtil`) for PIN hashing
* Build: Maven

---

## Requirements

* Java JDK 17+ installed (`JAVA_HOME` set).
* Maven or Gradle if you prefer CLI builds (IDE runs are also supported).
* Terminal that supports ANSI colors (recommended for best UX).

---

## Build & run

### Using Maven

```bash
# Build (from project root)
mvn clean package

# Run (normal)
java java -jar target/pocket-bank-1.0-SNAPSHOT.jar
```

### Run from your IDE

* Import as a Maven/Gradle project into IntelliJ IDEA or Eclipse.
* Run (`com.banking.Main`)

---

## CLI quick reference & sample session

**Global shortcuts:**
`q` / `quit` → quit, `b` / `back` → previous menu, `h` → help

**Main menu (typical):**

1. Accounts & Balances
2. Transaction History
3. Withdraw
4. Deposit
5. Transfer
6. Change PIN
7. Logout

---

## Database schema (concise)

Representative H2 tables used by the app:

**users**

```sql
user_id UUID PRIMARY KEY,
username VARCHAR(50) UNIQUE NOT NULL,
pin_hash VARCHAR(255) NOT NULL,
locked BOOLEAN DEFAULT FALSE,
failed_login_attempts INT DEFAULT 0,
role VARCHAR(20) DEFAULT 'USER'
```

**accounts**

```sql
account_id UUID PRIMARY KEY,
user_id UUID NOT NULL,
account_number VARCHAR(20) UNIQUE NOT NULL,
balance DECIMAL(15,2) DEFAULT 0,
currency VARCHAR(10),
is_active BOOLEAN DEFAULT TRUE,
FOREIGN KEY (user_id) REFERENCES users(user_id)
```

**transactions**

```sql
transaction_id UUID PRIMARY KEY,
account_id UUID NOT NULL,
type VARCHAR(20) NOT NULL,
amount DECIMAL(15,2) NOT NULL,
balance_after DECIMAL(15,2) NOT NULL,
description VARCHAR(255),
dest_account_id UUID,
timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
FOREIGN KEY (account_id) REFERENCES accounts(account_id),
FOREIGN KEY (dest_account_id) REFERENCES accounts(account_id)
```

---

## Design & security notes

* **PIN handling:** PINs are hashed (BCrypt). No plain PINs stored.
* **Login protection:** failed attempts tracked; configurable lockout to prevent brute-force.
* **Atomicity:** transfers and balance updates use JDBC transactions (`setAutoCommit(false)` + `commit()` / `rollback()`) to maintain consistency.
* **UUIDs:** use UUIDs for stable, non-guessable identifiers.
* **Why JDBC:** explicit SQL and transaction handling make transactional behavior and DB mapping transparent.

---

## Project structure (high level)

```
src/
 ├─ main/java/com/banking/cli         # CLI (MenuRenderer, InputReader, CliApp)
 ├─ main/java/com/banking/models      # User, Account, Transaction
 ├─ main/java/com/banking/repositories# JDBC repositories (UserRepo, AccountRepo, TxRepo)
 ├─ main/java/com/banking/services    # AuthService, AccountService, TransactionService
 └─ main/java/com/banking/utils       # CryptoUtil, ValidationUtil, GeneralUtils
```

---

## Future improvements

* Add role-based admin console and audit viewer.
* Add CI with unit + integration tests and coverage reporting.
* Persistent encrypted DB or credentials vault for production-like demos.
* Add user sessions persisted to disk or a session store.

---

## License & contact

* **License:** MIT.
* **Developer:** Raimi Dikamona Lassissi
* **GitHub:** https://github.com/ray-cod/pocket-bank.git
* **Email:** rdikamona9@gmail.com
