# PromoQuoter

A Spring Boot service that prices shopping carts with pluggable promotion rules and confirms orders
by reserving stock atomically, safely under concurrent and repeated requests.

- [Quick start](#quick-start)
- [API walkthrough](#api-walkthrough)
- [API reference](#api-reference)
- [Design](#design)
- [Pricing rules](#pricing-rules)
- [Concurrency and idempotency](#concurrency-and-idempotency)
- [Assumptions](#assumptions)
- [Testing and coverage](#testing-and-coverage)
- [Project layout](#project-layout)
- [What I would do next](#what-i-would-do-next)

## Quick start

Requires **JDK 17 or newer** (CI builds on 17 and 21). Maven is not required; the wrapper downloads it.

```bash
./mvnw spring-boot:run        # or: mvn spring-boot:run
```

The service listens on `http://localhost:8080`. Data lives in an in-memory H2 database and resets on
restart. The H2 console is at `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:mem:promoquoter`, user `sa`, no password).

```bash
./mvnw verify                 # unit + integration tests, coverage report and coverage gate
open target/site/jacoco/index.html
```

| | |
|---|---|
| Language | Java 17+ (built and tested on 17 and 21) |
| Framework | Spring Boot 3.5 (Web, Data JPA, Validation) |
| Database | H2, in memory |
| Tests | JUnit 5, AssertJ, MockMvc, TestRestTemplate |
| Coverage | JaCoCo, build fails below 90% lines / 85% branches |
| CI | GitHub Actions runs `./mvnw verify` on Java 17 and 21 for every push and pull request |

## API walkthrough

The commands below are copy-paste ready (they use [`jq`](https://jqlang.org) to capture ids). The responses shown are from a
real run, shortened where marked.

**Prefer Postman?** Import [`docs/postman/PromoQuoter.postman_collection.json`](docs/postman/PromoQuoter.postman_collection.json)
and use *Run collection*. Its 13 requests follow this walkthrough, pass ids between requests automatically
and check every response.

### 1. Create products

```bash
PRODUCTS=$(curl -s -X POST localhost:8080/products -H 'Content-Type: application/json' -d '[
  {"name": "Laptop", "category": "electronics", "price": 1000.00, "stock": 10},
  {"name": "Mouse",  "category": "electronics", "price": 25.00,   "stock": 100},
  {"name": "Novel",  "category": "books",       "price": 15.99,   "stock": 5}
]')
LAPTOP=$(echo "$PRODUCTS" | jq -r '.[0].id')
MOUSE=$(echo "$PRODUCTS"  | jq -r '.[1].id')
NOVEL=$(echo "$PRODUCTS"  | jq -r '.[2].id')
```

```json
[
  {"id": "883d9f54-…", "name": "Laptop", "category": "ELECTRONICS", "price": 1000.00, "stock": 10},
  {"id": "cceb515a-…", "name": "Mouse",  "category": "ELECTRONICS", "price": 25.00,   "stock": 100},
  {"id": "708753c4-…", "name": "Novel",  "category": "BOOKS",       "price": 15.99,   "stock": 5}
]
```

A single object (not wrapped in an array) is also accepted.

### 2. Create promotions

```bash
curl -s -X POST localhost:8080/promotions -H 'Content-Type: application/json' -d '[
  {"type": "PERCENT_OFF_CATEGORY", "name": "Electronics 10% off", "category": "electronics", "percent": 10},
  {"type": "TIERED_BULK_DISCOUNT", "name": "Mouse bulk discount", "productId": "'$MOUSE'",
   "tiers": [{"minQuantity": 10, "percent": 5}, {"minQuantity": 50, "percent": 10}]},
  {"type": "BUY_X_GET_Y", "name": "Laptops: buy 2 get 1 free", "productId": "'$LAPTOP'",
   "buyQuantity": 2, "freeQuantity": 1},
  {"type": "PERCENT_OFF_CATEGORY", "name": "VIP books 20% off", "category": "books", "percent": 20,
   "customerSegments": ["VIP"]}
]'
```

### 3. Quote a cart

```bash
curl -s -X POST localhost:8080/cart/quote -H 'Content-Type: application/json' -d '{
  "items": [
    {"productId": "'$LAPTOP'", "qty": 3},
    {"productId": "'$MOUSE'",  "qty": 12},
    {"productId": "'$NOVEL'",  "qty": 1}
  ],
  "customerSegment": "REGULAR"
}'
```

```json
{
  "customerSegment": "REGULAR",
  "lines": [
    {
      "productId": "883d9f54-…", "name": "Laptop", "category": "ELECTRONICS",
      "unitPrice": 1000.00, "qty": 3, "subtotal": 3000.00,
      "discounts": [
        {"promotionId": "f2845caa-…", "promotionName": "Electronics 10% off",       "amount": 300.00},
        {"promotionId": "fe9cc950-…", "promotionName": "Laptops: buy 2 get 1 free", "amount": 900.00}
      ],
      "discountTotal": 1200.00, "total": 1800.00
    },
    {
      "productId": "cceb515a-…", "name": "Mouse", "category": "ELECTRONICS",
      "unitPrice": 25.00, "qty": 12, "subtotal": 300.00,
      "discounts": [
        {"promotionId": "f2845caa-…", "promotionName": "Electronics 10% off", "amount": 30.00},
        {"promotionId": "f73a0afd-…", "promotionName": "Mouse bulk discount", "amount": 13.50}
      ],
      "discountTotal": 43.50, "total": 256.50
    },
    {
      "productId": "708753c4-…", "name": "Novel", "category": "BOOKS",
      "unitPrice": 15.99, "qty": 1, "subtotal": 15.99,
      "discounts": [], "discountTotal": 0.00, "total": 15.99
    }
  ],
  "appliedPromotions": [
    {"order": 1, "promotionId": "f2845caa-…", "name": "Electronics 10% off",       "type": "PERCENT_OFF_CATEGORY", "discount": 330.00},
    {"order": 2, "promotionId": "f73a0afd-…", "name": "Mouse bulk discount",       "type": "TIERED_BULK_DISCOUNT", "discount": 13.50},
    {"order": 3, "promotionId": "fe9cc950-…", "name": "Laptops: buy 2 get 1 free", "type": "BUY_X_GET_Y",          "discount": 900.00}
  ],
  "subtotal": 3315.99,
  "discountTotal": 1243.50,
  "total": 2072.49
}
```

The laptop line shows the ordering: 10% comes off first (3000.00 → 2700.00), then one of the three
laptops is free at the discounted unit price (900.00). The VIP-only promotion is skipped for a `REGULAR`
customer; quoting the novel with `"customerSegment": "VIP"` gives `3.20` off (`total: 12.79`).

### 4. Confirm the order

```bash
curl -s -i -X POST localhost:8080/cart/confirm -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: 7f1c2a9e-checkout-1' \
  -d '{"items": [{"productId": "'$LAPTOP'", "qty": 3}], "customerSegment": "REGULAR"}'
```

```http
HTTP/1.1 201
Idempotent-Replayed: false

{"orderId": "a0549157-…", "createdAt": "2026-11-16T18:28:36.973Z", "customerSegment": "REGULAR",
 "lines": [ … ], "appliedPromotions": [ … ], "subtotal": 3000.00, "discountTotal": 1200.00, "total": 1800.00}
```

Laptop stock drops from 10 to 7 (`curl localhost:8080/products/$LAPTOP`).

### 5. Retry with the same key

Sending exactly the same request again returns the **same order** and does not reserve stock again:

```http
HTTP/1.1 201
Idempotent-Replayed: true

{"orderId": "a0549157-…", …}
```

Reusing the key with a different cart is rejected:

```json
{"type": "about:blank", "title": "Idempotency key reused", "status": 422,
 "detail": "Idempotency-Key '7f1c2a9e-checkout-1' was already used for a different request.",
 "instance": "/cart/confirm", "code": "IDEMPOTENCY_KEY_REUSED"}
```

### 6. Out of stock

```bash
curl -s -X POST localhost:8080/cart/confirm -H 'Content-Type: application/json' \
  -d '{"items": [{"productId": "'$NOVEL'", "qty": 6}]}'
```

```json
{"type": "about:blank", "title": "Insufficient stock", "status": 409,
 "detail": "Not enough stock for 'Novel': requested 6, available 5.",
 "instance": "/cart/confirm", "code": "OUT_OF_STOCK",
 "shortages": [{"productId": "708753c4-…", "name": "Novel", "requested": 6, "available": 5}]}
```

### 7. Invalid input

```bash
curl -s -X POST localhost:8080/cart/quote -H 'Content-Type: application/json' \
  -d '{"items": [{"productId": "'$NOVEL'", "qty": 0}]}'
```

```json
{"type": "about:blank", "title": "Validation failed", "status": 400,
 "detail": "The request contains 1 invalid value(s).", "instance": "/cart/quote",
 "code": "VALIDATION_FAILED", "errors": [{"field": "items[0].qty", "message": "must be greater than 0"}]}
```

## API reference

| Method | Path | Body | Success | Notes |
|---|---|---|---|---|
| `POST` | `/products` | product or array of up to 100 | `201` + created products | All or nothing |
| `GET` | `/products/{id}` | | `200` | Handy for checking stock after a confirm |
| `POST` | `/promotions` | promotion or array of up to 100 (types may be mixed) | `201` + created promotions | All or nothing |
| `GET` | `/promotions` | | `200` | In creation order |
| `POST` | `/cart/quote` | `{items: [{productId, qty}], customerSegment?}` | `200` + itemised price | Read-only |
| `POST` | `/cart/confirm` | same as quote; optional `Idempotency-Key` header | `201` + `orderId` + itemised price | Reserves stock |

**Promotion bodies**, chosen by `type`. All take `name` and optional `customerSegments` (`REGULAR`, `PREMIUM`, `VIP`).

| `type` | Fields |
|---|---|
| `PERCENT_OFF_CATEGORY` | `category`, `percent` |
| `TIERED_BULK_DISCOUNT` | `productId`, `tiers: [{minQuantity, percent}]` (1-10 tiers, distinct thresholds) |
| `BUY_X_GET_Y` | `productId`, `buyQuantity`, `freeQuantity` |

`percent` must be greater than 0 and at most 100, with up to 2 decimals. Prices have up to 2 decimals.

**Errors** are [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457) (formerly RFC 7807) problem details
(`application/problem+json`) with a stable `code`:

| Status | `code` | When |
|---|---|---|
| 400 | `VALIDATION_FAILED` | Bean Validation failed; `errors` lists each `field` and `message` |
| 400 | `MALFORMED_REQUEST` | Invalid JSON, unknown property, wrong value type, unknown `type` or enum value |
| 4xx | HTTP status name | Other request problems raised by Spring MVC use the HTTP status name as `code` (`NOT_FOUND`, `METHOD_NOT_ALLOWED`, `UNSUPPORTED_MEDIA_TYPE`, ...) |
| 404 | `RESOURCE_NOT_FOUND` | `GET /products/{id}` with an unknown id |
| 409 | `OUT_OF_STOCK` | Confirm would oversell; `shortages` lists every short product |
| 409 | `CONCURRENT_UPDATE` | A row lock timed out or a version check failed; safe to retry |
| 409 | `IDEMPOTENCY_KEY_IN_PROGRESS` | Another request with the same key has not finished |
| 422 | `UNKNOWN_PRODUCT` | A cart or promotion refers to a product that does not exist |
| 422 | `IDEMPOTENCY_KEY_REUSED` | The key was already used for a different cart |
| 500 | `INTERNAL_ERROR` | Unexpected failure (details are logged, never returned) |

## Design

```
                       ┌──────────────────────── pricing (no Spring Web / JPA dependencies) ───────────────────────┐
 Promotion definitions │  PromotionRuleFactory ──► PromotionRule ──► AuditingPromotionRule ──► PromotionPipeline   │ ──► PricingResult
 (JPA, single table)   │   (one provider/type)      (strategy)          (decorator)             (ordered stages)  │
                       └───────────────────────────────────────────────────────────────────────────────────────────┘
```

| Pattern | Where | Why |
|---|---|---|
| **Strategy** | `PromotionRule` with `PercentOffCategoryRule`, `TieredBulkDiscountRule`, `BuyXGetYRule` | Each rule owns its algorithm and decides which lines it touches |
| **Pipeline** | `PromotionPipeline` | Runs rules by `priority()`; it knows nothing about specific types |
| **Factory** | `PromotionRuleFactory` + one `PromotionRuleProvider` bean per type | Builds rules from stored definitions without a growing `switch` |
| **Decorator** | `AuditingPromotionRule` | Records the cart total before and after each rule; `appliedPromotions` comes from this audit trail |

**Adding a promotion type** (for example, a shipping waiver) means adding three things:

1. a definition entity extending `Promotion` and a request record in `CreatePromotionRequest`,
2. a rule implementing `PromotionRule` with a `priority()` from `RulePriority`,
3. a `PromotionRuleProvider` bean for the definition.

`PromotionPipeline`, `PricingEngine` and the factory do not change. `PromoQuoterApplicationIT` fails
if a promotion entity has no provider, so a missing step is caught in CI.

The pricing package is plain Java with no web or persistence code, so it is unit tested without starting Spring.
`CartPricingService` adapts it to the catalogue, and `CartService` adds the transaction and locking for confirm.

## Pricing rules

- **Order.** Rules run in stages: category percentage (100), tiered bulk percentage (200),
  buy X get Y (300). Within a stage, promotions apply in creation order. This is the "category discount
  before BxGy" order from the brief, and it means free units are valued at the discounted price.
- **Stacking.** Every applicable promotion applies. Each one works on what is left of the line after the
  earlier ones, so two 10% category promotions give 19% off, not 20%.
- **Buy X get Y.** Free units must be in the cart. Out of every complete group of `X + Y` units, `Y` are
  free: with buy 2 get 1, 3 units give 1 free, 5 give 1, 6 give 2.
- **Tiered bulk.** Only the highest tier reached applies; tiers do not add up.
- **Segments.** A promotion with `customerSegments` applies only to those segments; without them it
  applies to everyone.
- **Rounding.** All money is `BigDecimal`. Each discount is rounded **HALF_UP to cents** when it is applied,
  so every discount shown is whole cents and `total` always equals the sum of the line totals.
  For example, 20% of 15.99 is 3.198, which rounds to 3.20 off, leaving 12.79.
- **Never negative.** A discount is capped at what is left on the line, so even a 100% discount plus
  buy 1 get 1 bottoms out at 0.00.

## Concurrency and idempotency

**Confirm is one transaction.** It locks the products, prices the cart, checks every line, reserves stock,
records the order and (with a key) records the idempotency result, all together or not at all.

- **Row locks.** Products are read with `PESSIMISTIC_WRITE` (`SELECT … FOR UPDATE`), so confirmations of
  the same product queue up and each sees the stock left by the previous one.
- **No deadlocks.** Rows are locked in id order, so two carts that share products lock them in the same order.
- **Second guard.** Products also carry a JPA `@Version`. Lock timeouts (5 s) and version conflicts map to
  `409 CONCURRENT_UPDATE`.
- **Why not optimistic locking alone?** With only `@Version`, overselling is still prevented, but valid
  orders fail with conflicts instead of waiting. In the concurrency test (25 buyers, 5 units) only 4 orders
  succeeded; with row locks exactly 5 do.
- **All or nothing.** Every line is checked before any stock is taken, and `409 OUT_OF_STOCK` lists every
  short product so the client can fix the cart in one go.

**Idempotency** follows the IETF
[`Idempotency-Key` header draft](https://datatracker.ietf.org/doc/draft-ietf-httpapi-idempotency-key-header/):

- The key is inserted as a primary key **in the same transaction** as the reservation, so the database
  allows only one request to claim it, and a key is never stored without its reservation (or vice versa).
- Concurrent requests with the same key wait on that row and then replay the winner's stored response.
- Requests are compared by a SHA-256 fingerprint of the segment and quantity per product, so a reordered
  but equivalent cart is the same request; a different cart gets `422`.
- Only successful confirmations are stored. If an attempt fails (for example, out of stock), the key is
  released and the client can retry with it.
- Replays return the original `201` body with `Idempotent-Replayed: true`.

## Assumptions

Decisions I made where the brief left room:

1. **Categories** are free text, matched case-insensitively and stored in upper case (`books` = `BOOKS`).
2. **`customerSegment`** is optional and defaults to `REGULAR`. Segments are `REGULAR`, `PREMIUM`, `VIP`.
3. **All matching promotions stack**; there is no "best single promotion" or exclusivity rule.
4. **Buy X get Y** gives free units only from what is already in the cart; it never adds items.
5. **Repeated cart lines** for the same product are combined before pricing and stock checks.
6. **Quote does not check or hold stock.** Confirm re-prices the cart under lock, so the confirmed price
   is authoritative if promotions or stock changed in between.
7. **One currency, no tax or shipping.** Amounts are plain decimals with two places.
8. **Referencing an unknown product** (in a cart or a promotion) is `422`, since the request is well-formed
   but cannot be processed. `GET /products/{id}` for an unknown id is `404`.
9. **Promotions are immutable** once created; update, delete and validity dates are out of scope.
10. **Idempotency keys** are global (there are no users) and do not expire, since data is in memory.
11. **Limits**: up to 100 items per batch or cart, and `qty` up to 10,000 per line.
12. **Unknown JSON properties are rejected** to catch typos such as `quantity` instead of `qty`.

## Testing and coverage

```bash
./mvnw verify
```

| Kind | Naming | Runs in | Examples |
|---|---|---|---|
| Unit | `*Test` | Surefire, no Spring context | Each rule, the pipeline, factory, decorator, rounding, the engine end to end |
| Integration | `*IT` | Failsafe, full app and H2 | Every endpoint through MockMvc, including validation and error bodies |
| Concurrency | `CartConfirmConcurrencyIT` | Real HTTP on a random port | 25 buyers for 5 units sell exactly 5; opposite-order carts do not deadlock; 10 retries with one key place one order |

146 tests; coverage is about 96% of lines and 90% of branches. The build fails if coverage drops below 90% of
lines or 85% of branches. The concurrency tests were checked by removing the row lock, and both
stock tests then fail.

Traceability from each line of the brief to its code and tests is in [`docs/REQUIREMENTS.md`](docs/REQUIREMENTS.md).

## Project layout

```
src/main/java/com/promoquoter
├── product       Product entity, catalogue endpoints, row-locking query
├── promotion     Promotion definitions (single-table inheritance), polymorphic requests, endpoints
├── pricing       Engine: context, pipeline, factory, audit decorator, Money
│   └── rules     One Strategy per promotion type, each with its provider
├── cart          Quote and confirm endpoints, cart pricing, stock reservation
├── order         Confirmed orders
├── idempotency   Idempotency-Key handling
└── common/error  RFC 9457 problem-detail error handling
```

## What I would do next

For a production version, in rough priority order:

- PostgreSQL with Flyway migrations instead of H2 with `ddl-auto`.
- Expiry for idempotency records (for example, 24 hours) and a cleanup job.
- Promotion lifecycle: validity dates, activation, update and delete, and an exclusivity flag.
- OpenAPI documentation, plus metrics and tracing for quote and confirm latency and lock waits.
- Authentication, with idempotency keys scoped per client.
- Order lookup and cancellation, which would release reserved stock.
