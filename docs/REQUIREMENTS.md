# Requirements traceability

Every requirement from the exercise brief, and where it is implemented and tested.

Status: ✅ done · 🚧 in progress · ⬜ not started

## Domain

| # | Requirement | Status | Implementation | Tests |
|---|---|---|---|---|
| D1 | Product: id, name, category, price (decimal), stock (int) | ⬜ | | |
| D2 | Promotion type `PERCENT_OFF_CATEGORY` | ⬜ | | |
| D3 | Promotion type `BUY_X_GET_Y` for a given product | ⬜ | | |
| D4 | Additional promotion type (optional) | ⬜ | | |
| D5 | Data stored in H2 / in-memory DB | ✅ | `application.yml` | `PromoQuoterApplicationIT` |

## Endpoints

| # | Requirement | Status | Implementation | Tests |
|---|---|---|---|---|
| E1 | `POST /products` creates product(s) | ⬜ | | |
| E2 | `POST /promotions` creates promotion(s) | ⬜ | | |
| E3 | `POST /cart/quote` accepts `items[{productId, qty}]` and `customerSegment` | ⬜ | | |
| E4 | Quote returns per-line and total breakdown, and applied promotions in order | ⬜ | | |
| E5 | `POST /cart/confirm` accepts the quote payload plus optional `Idempotency-Key` header | ⬜ | | |
| E6 | Confirm validates stock and reserves it atomically | ⬜ | | |
| E7 | Confirm returns the final price and a generated `orderId` | ⬜ | | |

## Rules engine

| # | Requirement | Status | Implementation | Tests |
|---|---|---|---|---|
| R1 | Promotions are pluggable and composable | ⬜ | | |
| R2 | Promotions apply in a defined order (category discount before BxGy) | ⬜ | | |
| R3 | Rules can be added, swapped or removed without touching orchestration code | ⬜ | | |
| R4 | Strategy pattern: each rule encapsulates its algorithm | ⬜ | | |
| R5 | Chain of Responsibility / Pipeline for ordered application | ⬜ | | |
| R6 | Factory: rules instantiated from persisted promotion definitions (optional) | ⬜ | | |
| R7 | Decorator: calculation context enriched with an audit trail (optional) | ⬜ | | |

## Concurrency and consistency

| # | Requirement | Status | Implementation | Tests |
|---|---|---|---|---|
| C1 | Confirm is safe under concurrent calls (optimistic or row-level pessimistic locking) | ⬜ | | |
| C2 | Out of stock returns `409 CONFLICT` with a clear error | ⬜ | | |
| C3 | Repeated confirm with the same `Idempotency-Key` does not reserve twice | ⬜ | | |

## Validation and errors

| # | Requirement | Status | Implementation | Tests |
|---|---|---|---|---|
| V1 | Input validated with Bean Validation | ⬜ | | |
| V2 | Structured error responses | ⬜ | | |
| V3 | Prices never go negative | ⬜ | | |
| V4 | Correct rounding (HALF_UP to cents) | ⬜ | | |

## Non-functional

| # | Requirement | Status | Implementation | Tests |
|---|---|---|---|---|
| N1 | Java 17+ | ✅ | Java 21 (`pom.xml`) | CI |
| N2 | Spring Boot 3.x | ✅ | 3.5.16 (`pom.xml`) | CI |
| N3 | Spring Web, Spring Data JPA, Validation, H2 runtime | ✅ | `pom.xml` | |
| N4 | Lean dependencies, no UI | ✅ | `pom.xml` | |

## Deliverables

| # | Requirement | Status | Where |
|---|---|---|---|
| X1 | Runnable via `mvn spring-boot:run` | ✅ | `./mvnw spring-boot:run` |
| X2 | Core endpoints implemented and tested | ⬜ | |
| X3 | Comprehensive unit and integration tests | ⬜ | |
| X4 | Code coverage | 🚧 | JaCoCo report at `target/site/jacoco` |
| X5 | README: assumptions, how to run, sample curl | 🚧 | `README.md` |
