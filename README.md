# Mini WSA

A Spring Boot service that ingests web security events, enriches each one with an attack type and a threat score, and serves stats and samples over a REST API.

**Stack:** Java 21, Spring Boot 4, Spring Data JPA, PostgreSQL 17, Flyway, Testcontainers, Docker Compose.

## 1. Build and run

Requires Docker (and Java 21+ for the local run and the generator).

```bash
# Everything in Docker: app on :8080, Postgres on :5433
docker compose up --build

# Or: DB in Docker, app locally
docker compose up -d db
./mvnw spring-boot:run

# Send 10,000 generated events
java generator/EventGenerator.java --count 10000

# Run all tests (Docker must be running)
./mvnw test
```

Endpoints: `POST /v1/events/ingest`, `GET /v1/stats/summary`, `GET /v1/events/samples`.

## 2. Architecture

![Architecture](https://github.com/user-attachments/assets/030595a5-d49b-47c0-b3e3-928a85ee8c81)

Three layers:
- **Controllers** handle HTTP. `ApiExceptionHandler` turns errors into ProblemDetail responses.
- **Services** hold the logic: validate, de-duplicate, enrich, build stats.
- **Repositories** run the SQL.

Each event in a batch is processed and saved on its own. One bad event never fails the whole request, and the response reports every event by its index. The enrichment (attack type and score) is computed once, at ingest, and stored with the event.

## 3. Storage choice: PostgreSQL

- **Fixed schema:** every event has the same shape, so a typed table with constraints fits.
- **Idempotency for free:** `eventId` is the primary key, so a resent event is detected as a duplicate.
- **Aggregations:** the stats are plain SQL (`GROUP BY`, `COUNT`, `AVG`, `ORDER BY … LIMIT`).
- **Indexes match the queries:**
  - `(client_ip, timestamp)` for the repeat-offender check,
  - `(config_id, timestamp)` for filtered stats and samples,
  - `(timestamp)` for unfiltered queries and newest-first order.
- **Simple to run and test:** one container, and Testcontainers runs the same Postgres in tests.

At real WAF scale (billions of append-only events), I would move to a columnar store such as ClickHouse.

## 4. What I would improve with more time

- **Columnar storage.** ClickHouse, partitioned by day. Aggregations would read only the columns they need, and old data could be dropped by partition.
- **Smarter enrichment.** Store the raw events fast and enrich them asynchronously (Kafka → enrichment consumers), instead of doing the work inside the ingest request. Batch inserts would raise throughput.
- **Repeat-offender check.** Today it runs one DB query per event and counts only what is already stored. So late or out-of-order events can score differently, and parallel requests can race. Better options:
  - a per-IP sliding-window counter in the stream (e.g. Redis), or
  - computing the count at read time with a SQL window function, which handles out-of-order data correctly.

## 5. Challenges and how I solved them

- **One bad event shouldn't fail the batch.** In Postgres, a single failed statement aborts the whole transaction. I process each event separately, give each its own transaction, and return a per-event report: `ingested`, `duplicates`, `rejected[index]`, `failed[index]`.
- **Clear error messages.** Wrong values (a bad enum, a bad date) fail inside the JSON parser, before validation runs. I parse each event on its own and convert those errors to the same `field: message` format as the validation errors.
- **A fair repeat-offender window.** "Last 10 minutes" is measured from the event's own timestamp, not from "now". That way, replayed or generated historical data still scores correctly.
