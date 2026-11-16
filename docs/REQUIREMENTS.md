# Requirements traceability

Every requirement from the exercise brief, and where it is implemented and tested.

Status: ✅ done · 🚧 in progress · ⬜ not started

## Domain

| # | Requirement | Status | Implementation | Tests |
|---|---|---|---|---|
| D1 | Product: id, name, category, price (decimal), stock (int) | ✅ | `product/Product` | `ProductTest`, `ProductApiIT` |
| D2 | Promotion type `PERCENT_OFF_CATEGORY` | ✅ | `PercentOffCategoryPromotion`, `pricing/rules/PercentOffCategoryRule` | `PromotionApiIT`, `PercentOffCategoryRuleTest` |
| D3 | Promotion type `BUY_X_GET_Y` for a given product | ✅ | `BuyXGetYPromotion`, `pricing/rules/BuyXGetYRule` | `PromotionApiIT`, `BuyXGetYRuleTest` |
| D4 | Additional promotion type (optional) | ✅ | `TIERED_BULK_DISCOUNT`: `TieredBulkDiscountPromotion`, `pricing/rules/TieredBulkDiscountRule` | `PromotionApiIT`, `TieredBulkDiscountRuleTest` |
| D5 | Data stored in H2 / in-memory DB | ✅ | `application.yml` | `PromoQuoterApplicationIT` |

## Endpoints

| # | Requirement | Status | Implementation | Tests |
|---|---|---|---|---|
| E1 | `POST /products` creates product(s) | ✅ | `product/ProductController` | `ProductApiIT` |
| E2 | `POST /promotions` creates promotion(s) | ✅ | `promotion/PromotionController` | `PromotionApiIT` |
| E3 | `POST /cart/quote` accepts `items[{productId, qty}]` and `customerSegment` | ⬜ | | |
| E4 | Quote returns per-line and total breakdown, and applied promotions in order | ⬜ | | |
| E5 | `POST /cart/confirm` accepts the quote payload plus optional `Idempotency-Key` header | ⬜ | | |
| E6 | Confirm validates stock and reserves it atomically | ⬜ | | |
| E7 | Confirm returns the final price and a generated `orderId` | ⬜ | | |

## Rules engine

| # | Requirement | Status | Implementation | Tests |
|---|---|---|---|---|
| R1 | Promotions are pluggable and composable | ✅ | `PromotionRule` + `PromotionRuleProvider` beans; rules compose on a shared `PricingContext` | `PromotionPipelineTest`, `PricingEngineTest` |
| R2 | Promotions apply in a defined order (category discount before BxGy) | ✅ | `RulePriority`: category (100) → volume (200) → free items (300); creation order breaks ties | `PromotionPipelineTest`, `PricingEngineTest` |
| R3 | Rules can be added, swapped or removed without touching orchestration code | ✅ | New type = definition + rule + provider bean; `PromotionPipeline` and `PricingEngine` unchanged | `PromotionPipelineTest.acceptsNewRuleTypesWithoutChanges`, `PromoQuoterApplicationIT` |
| R4 | Strategy pattern: each rule encapsulates its algorithm | ✅ | `pricing/PromotionRule`, `pricing/rules/*` | rule unit tests |
| R5 | Chain of Responsibility / Pipeline for ordered application | ✅ | `pricing/PromotionPipeline` | `PromotionPipelineTest` |
| R6 | Factory: rules instantiated from persisted promotion definitions (optional) | ✅ | `pricing/PromotionRuleFactory` | `PromotionRuleFactoryTest` |
| R7 | Decorator: calculation context enriched with an audit trail (optional) | ✅ | `pricing/AuditingPromotionRule` → `AuditEntry` | `AuditingPromotionRuleTest` |

## Concurrency and consistency

| # | Requirement | Status | Implementation | Tests |
|---|---|---|---|---|
| C1 | Confirm is safe under concurrent calls (optimistic or row-level pessimistic locking) | ⬜ | | |
| C2 | Out of stock returns `409 CONFLICT` with a clear error | ⬜ | | |
| C3 | Repeated confirm with the same `Idempotency-Key` does not reserve twice | ⬜ | | |

## Validation and errors

| # | Requirement | Status | Implementation | Tests |
|---|---|---|---|---|
| V1 | Input validated with Bean Validation | 🚧 | `CreateProductRequest`, `CreatePromotionRequest`, `@Percentage` | `ProductApiIT`, `PromotionApiIT` |
| V2 | Structured error responses | ✅ | `common/error/GlobalExceptionHandler` (RFC 7807 + `code` + `errors`) | `ProductApiIT` |
| V3 | Prices never go negative | ✅ | `LineItem.applyDiscount` caps each discount at the line total | `PricingContextTest`, `PricingEngineTest` |
| V4 | Correct rounding (HALF_UP to cents) | ✅ | `pricing/Money` | `MoneyTest`, rule tests |

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
