# JL Enterprises — E-Commerce Backend

A production-grade, modular-monolith e-commerce backend.

**Stack:** Java 21 · Spring Boot 3.3 · Spring Security 6 · Spring Data JPA / Hibernate ·
**PostgreSQL** · Redis · Flyway · MapStruct · Lombok · JWT (access + refresh) ·
springdoc OpenAPI · Spring Mail · Docker · Maven · JUnit 5 + Mockito + Testcontainers.

> The single backend for the project. The static admin site (`frontend/`) calls its
> `/api/v1/auth` API. Per decision, it targets **PostgreSQL** (not the spec's MySQL)
> so it can align with / migrate onto the store's Supabase Postgres later. It runs on
> **port 8081** locally.

---

## Architecture

Cross-cutting concerns live in technical packages; business logic is grouped by
domain within the service/repository layers.

```
in.jlenterprises.ecommerce
├── config           app-wide @Configuration (JPA auditing, OpenAPI, CORS, Redis, async)
├── security         Spring Security 6 config + entry point / access-denied handlers
│   ├── jwt          access/refresh token service
│   └── filter       JWT auth, rate-limit, request-logging filters
├── controller       thin REST controllers (domain sub-packages)
├── service /impl    business contracts + implementations
├── repository       Spring Data JPA repos + Specifications
├── entity           JPA entities + BaseEntity (never exposed)
├── dto / request    read models / validated command objects
├── response         ApiResponse, PageResponse envelopes
├── mapper           MapStruct entity↔DTO mappers
├── exception        custom exceptions + global handler
├── validation       custom constraints (password, phone, OTP, coupon)
├── payment          Strategy-pattern payment abstraction + provider adapters
├── notification     email / SMS / push senders
├── audit            audit logging (AOP)
└── event / listener / scheduler / cache / util / constant / file
```

All API responses use one envelope:
```json
{ "success": true, "message": "OK", "data": {}, "timestamp": "2026-07-03T10:00:00Z" }
```

---

## Run locally

**1. Start infrastructure** (Postgres + Redis + MailHog):
```bash
cd ecommerce-backend
docker compose up -d db redis mailhog
```

**2. Run the app** from your IDE or Maven:
```bash
mvn spring-boot:run
```
- API: http://localhost:8081
- Swagger UI: http://localhost:8081/swagger-ui.html
- Health: http://localhost:8081/actuator/health
- MailHog inbox: http://localhost:8025

**Or run everything in Docker** (builds the image too):
```bash
docker compose up --build
```

Set a real `JWT_SECRET` (≥ 32 bytes) before anything but local dev:
```bash
export JWT_SECRET=$(openssl rand -base64 48)
```

---

## The compile gate

This project is delivered **module by module**; each milestone must build before
the next starts. Verify a milestone with:

```bash
mvn -q -DskipTests compile      # compiles (fast)
mvn -q test                     # compiles + runs tests
```

> Heads-up: these were authored in an environment without a JDK, so **you** run
> the gate. Report any compiler error and it gets fixed before we move on.

---

## Delivery roadmap

- [x] **Steps 1–3** — folder structure, `pom.xml`, `application.yml`, Docker, response envelopes
- [x] **Steps 4–7** — entities, repositories + Specifications, DTOs, MapStruct mappers
- [x] **Steps 8–10** — Spring Security, JWT (access+refresh), full auth module
- [x] **Steps 11–12** — customer (address, notifications) + admin (users, roles, dashboard, audit view) APIs
- [x] **Steps 13–17** — product/category/brand, cart, wishlist, order (inventory+coupon+invoice), coupon
- [x] **Steps 18–19** — payment (Strategy pattern: COD live, Stripe/Razorpay/PayPal stubs) + reviews
- [x] **Step 20** — audit logging (AOP `@Auditable` + request-logging filter)
- [x] **Step 21** — tests (JUnit unit, Mockito service, Testcontainers repository IT)

### Known follow-ups
- **Freeze the schema into Flyway.** The app currently runs with `ddl-auto=update` and Flyway paused
  (`FLYWAY_ENABLED=false`) — the entity-development mode from Step 4. To productionise: start a clean DB,
  dump the Hibernate-generated DDL into `db/migration/V2__schema.sql`, then set `JPA_DDL_AUTO=validate`
  and `FLYWAY_ENABLED=true`.
- **Real payment SDKs** — replace the Stripe/Razorpay/PayPal strategy stubs (marked `TODO`) with the
  provider SDK calls; COD works as-is.
- **PDF invoices** — `InvoiceDto` returns invoice data as JSON; wire a PDF renderer if needed.

---

## Roles
`ROLE_SUPER_ADMIN` · `ROLE_ADMIN` · `ROLE_MANAGER` · `ROLE_CUSTOMER`
