# PromoQuoter

Cart pricing and inventory reservation microservice.

- Prices a cart from products and pluggable promotion rules, returning an itemised breakdown.
- Confirms an order by reserving stock atomically, safe under concurrent and repeated requests.

## Tech stack

| | |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5 (Web, Data JPA, Validation) |
| Database | H2 (in-memory) |
| Build | Maven (wrapper included) |
| Tests | JUnit 5, AssertJ, MockMvc |
| Coverage | JaCoCo |

## Running

Requires JDK 17 or newer.

```bash
./mvnw spring-boot:run
```

The service starts on `http://localhost:8080`.

## Testing

```bash
./mvnw verify
```

Runs unit tests (`*Test`) and integration tests (`*IT`), then writes the coverage report to
`target/site/jacoco/index.html`.
