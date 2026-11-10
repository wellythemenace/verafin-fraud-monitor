# Fraud Monitor

Transaction monitoring system for detecting suspicious deposit activity, built with Java and Spring Boot.
It flags suspicious deposits with explainable rules and keeps an alert queue for analyst review.

**Stack:** Java 17, Spring Boot 3, Spring Data JPA, PostgreSQL, JUnit/Mockito

## Detection rules

| Rule | What it catches |
|------|-----------------|
| **Structuring** | 2+ deposits within 24 hours, each under the $10,000 reporting threshold, that add up to $10,000 or more |
| **Profile deviation (KYC)** | Deposits over 30 days far above what the customer is expected to deposit (5x, or 2x for HIGH risk customers) |

- Each alert stores the rule name, a risk score, and a plain-English reason.
- **Auto-clearing:** if a source of funds is on record (e.g. sale of a house), a profile-deviation alert is logged as `AUTO_CLEARED` instead of going to the analyst queue. It is kept for audit.
- Rules share a `FraudRule` interface, so adding a new rule is one new class.

## API

| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | `/accounts` | Create an account, optionally with a KYC profile |
| PUT | `/accounts/{id}/profile` | Update expected deposits, risk rating, source of funds |
| POST | `/transactions` | Record a transaction and run all rules |
| GET | `/transactions?accountId=` | List an account's transactions |
| GET | `/alerts?status=OPEN` | List alerts (OPEN, AUTO_CLEARED, ...) |

## Run locally

1. Install PostgreSQL and create a database: `createdb fraud_monitor`
2. Set your database username/password in `src/main/resources/application.properties`
3. Run `FraudMonitorApplication` (tables are created from `schema.sql`)
4. Example:
   ```
   curl -X POST localhost:8080/accounts -H "Content-Type: application/json" -d '{"customerName":"Test","expectedMonthlyDeposits":3000}'
   curl -X POST localhost:8080/transactions -H "Content-Type: application/json" -d '{"accountId":1,"amount":20000,"type":"DEPOSIT","occurredAt":"2026-11-10T09:00:00"}'
   curl localhost:8080/alerts
   ```

## Planned
- Velocity rule (too many transactions in a short window)
- ML risk score alongside the rules
- AI investigator that drafts a case summary for the analyst, who makes the final decision
- Deployment to AWS

## Limitations
- Alerts can repeat for the same account on later deposits
- Source of funds is a free-text note, not verified documents
- Thresholds are fixed values, not tuned on real data
