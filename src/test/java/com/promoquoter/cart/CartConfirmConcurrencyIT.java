package com.promoquoter.cart;

import com.jayway.jsonpath.JsonPath;
import com.promoquoter.order.OrderRepository;
import com.promoquoter.product.Product;
import com.promoquoter.product.ProductRepository;
import com.promoquoter.support.DatabaseCleaner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fires real concurrent HTTP requests at a running server to prove that stock is never oversold.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(DatabaseCleaner.class)
class CartConfirmConcurrencyIT {

    private static final int THREADS = 25;

    @Autowired
    private TestRestTemplate http;

    @Autowired
    private ProductRepository products;

    @Autowired
    private OrderRepository orders;

    @Autowired
    private DatabaseCleaner databaseCleaner;

    @BeforeEach
    void cleanDatabase() {
        databaseCleaner.clean();
    }

    @Test
    void neverSellsMoreThanIsInStock() throws Exception {
        Product product = products.save(new Product("Console", "electronics", new BigDecimal("499.00"), 5));
        String body = cartJson(Map.of(product, 1));

        List<ResponseEntity<String>> responses = runConcurrently(THREADS, () -> confirm(body));

        assertThat(responses).filteredOn(r -> r.getStatusCode() == HttpStatus.CREATED).hasSize(5);
        assertThat(responses).filteredOn(r -> r.getStatusCode() == HttpStatus.CONFLICT).hasSize(THREADS - 5)
                .allSatisfy(r -> assertThat(r.getBody()).contains("\"code\":\"OUT_OF_STOCK\""));
        assertThat(products.findById(product.getId()).orElseThrow().getStock()).isZero();
        assertThat(orders.count()).isEqualTo(5);
    }

    @Test
    void cartsLockingSharedProductsInOppositeOrderDoNotDeadlock() throws Exception {
        Product first = products.save(new Product("First", "misc", new BigDecimal("1.00"), 100));
        Product second = products.save(new Product("Second", "misc", new BigDecimal("2.00"), 100));
        String forward = "{\"items\":[{\"productId\":\"%s\",\"qty\":1},{\"productId\":\"%s\",\"qty\":1}]}"
                .formatted(first.getId(), second.getId());
        String backward = "{\"items\":[{\"productId\":\"%s\",\"qty\":1},{\"productId\":\"%s\",\"qty\":1}]}"
                .formatted(second.getId(), first.getId());

        List<Callable<ResponseEntity<String>>> tasks = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            String body = i % 2 == 0 ? forward : backward;
            tasks.add(() -> confirm(body));
        }
        List<ResponseEntity<String>> responses = runConcurrently(tasks);

        assertThat(responses).allSatisfy(r -> assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED));
        assertThat(products.findById(first.getId()).orElseThrow().getStock()).isEqualTo(100 - THREADS);
        assertThat(products.findById(second.getId()).orElseThrow().getStock()).isEqualTo(100 - THREADS);
    }

    @Test
    void concurrentRetriesWithTheSameKeyPlaceOneOrder() throws Exception {
        Product product = products.save(new Product("Headphones", "electronics", new BigDecimal("99.00"), 10));
        String body = cartJson(Map.of(product, 2));

        List<ResponseEntity<String>> responses = runConcurrently(10, () -> confirm(body, "checkout-42"));

        assertThat(responses).allSatisfy(r -> assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED));
        assertThat(responses).extracting(r -> JsonPath.<String>read(r.getBody(), "$.orderId")).containsOnly(
                JsonPath.<String>read(responses.getFirst().getBody(), "$.orderId"));
        assertThat(responses).filteredOn(r -> "false".equals(r.getHeaders().getFirst("Idempotent-Replayed")))
                .hasSize(1);
        assertThat(products.findById(product.getId()).orElseThrow().getStock()).isEqualTo(8);
        assertThat(orders.count()).isEqualTo(1);
    }

    private ResponseEntity<String> confirm(String body) {
        return confirm(body, null);
    }

    private ResponseEntity<String> confirm(String body, String idempotencyKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (idempotencyKey != null) {
            headers.set("Idempotency-Key", idempotencyKey);
        }
        return http.postForEntity("/cart/confirm", new HttpEntity<>(body, headers), String.class);
    }

    private static List<ResponseEntity<String>> runConcurrently(int count, Callable<ResponseEntity<String>> task)
            throws Exception {
        List<Callable<ResponseEntity<String>>> tasks = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            tasks.add(task);
        }
        return runConcurrently(tasks);
    }

    /** Starts all tasks at the same moment to maximise contention. */
    private static List<ResponseEntity<String>> runConcurrently(List<Callable<ResponseEntity<String>>> tasks)
            throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(tasks.size());
        CountDownLatch startGate = new CountDownLatch(1);
        try {
            List<Future<ResponseEntity<String>>> futures = new ArrayList<>();
            for (Callable<ResponseEntity<String>> task : tasks) {
                futures.add(executor.submit(() -> {
                    startGate.await();
                    return task.call();
                }));
            }
            startGate.countDown();
            List<ResponseEntity<String>> results = new ArrayList<>();
            for (Future<ResponseEntity<String>> future : futures) {
                results.add(future.get(30, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            executor.shutdownNow();
        }
    }

    private static String cartJson(Map<Product, Integer> quantities) {
        StringBuilder items = new StringBuilder();
        quantities.forEach((product, qty) -> {
            if (!items.isEmpty()) {
                items.append(',');
            }
            items.append("{\"productId\":\"").append(product.getId()).append("\",\"qty\":").append(qty).append('}');
        });
        return "{\"items\":[" + items + "]}";
    }
}
