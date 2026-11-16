package com.promoquoter.idempotency;

import com.jayway.jsonpath.JsonPath;
import com.promoquoter.order.OrderRepository;
import com.promoquoter.product.Product;
import com.promoquoter.product.ProductRepository;
import com.promoquoter.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class IdempotentConfirmApiIT extends IntegrationTest {

    @Autowired
    private ProductRepository products;

    @Autowired
    private OrderRepository orders;

    @Autowired
    private IdempotencyRecordRepository records;

    private Product laptop;
    private Product mouse;

    @BeforeEach
    void seedCatalogue() {
        laptop = products.save(new Product("Laptop", "electronics", new BigDecimal("1000.00"), 10));
        mouse = products.save(new Product("Mouse", "electronics", new BigDecimal("25.00"), 10));
    }

    @Test
    void repeatingARequestWithTheSameKeyReturnsTheOriginalOrderWithoutReservingAgain() throws Exception {
        String body = cart(laptop, 2);

        String first = confirm(body, "order-123")
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotent-Replayed", "false"))
                .andReturn().getResponse().getContentAsString();

        confirm(body, "order-123")
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotent-Replayed", "true"))
                .andExpect(content().json(first, true));

        assertThat(stockOf(laptop)).isEqualTo(8);
        assertThat(orders.count()).isEqualTo(1);
    }

    @Test
    void treatsAnEquivalentCartAsTheSameRequest() throws Exception {
        String original = """
                {"items": [{"productId": "%s", "qty": 1}, {"productId": "%s", "qty": 2}]}
                """.formatted(laptop.getId(), mouse.getId());
        String reordered = """
                {"items": [{"productId": "%s", "qty": 2}, {"productId": "%s", "qty": 1}], "customerSegment": "REGULAR"}
                """.formatted(mouse.getId(), laptop.getId());

        String orderId = orderIdOf(confirm(original, "key-1").andExpect(status().isCreated()));

        confirm(reordered, "key-1")
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotent-Replayed", "true"))
                .andExpect(jsonPath("$.orderId").value(orderId));
    }

    @Test
    void rejectsReusingAKeyForADifferentCart() throws Exception {
        confirm(cart(laptop, 1), "key-2").andExpect(status().isCreated());

        confirm(cart(laptop, 3), "key-2")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_REUSED"))
                .andExpect(jsonPath("$.detail").value("Idempotency-Key 'key-2' was already used for a different request."));

        assertThat(stockOf(laptop)).isEqualTo(9);
    }

    @Test
    void doesNotRememberFailedRequestsSoTheKeyCanBeRetried() throws Exception {
        confirm(cart(laptop, 11), "key-3").andExpect(status().isConflict());

        assertThat(records.count()).isZero();

        confirm(cart(laptop, 10), "key-3")
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotent-Replayed", "false"));
        assertThat(stockOf(laptop)).isZero();
    }

    @Test
    void requestsWithoutAKeyAreNeverDeduplicated() throws Exception {
        String body = cart(laptop, 1);

        postJson("/cart/confirm", body)
                .andExpect(status().isCreated())
                .andExpect(header().doesNotExist("Idempotent-Replayed"));
        postJson("/cart/confirm", body).andExpect(status().isCreated());

        assertThat(orders.count()).isEqualTo(2);
        assertThat(stockOf(laptop)).isEqualTo(8);
    }

    @Test
    void rejectsMalformedKeys() throws Exception {
        confirm(cart(laptop, 1), "has spaces")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("Idempotency-Key"));
        confirm(cart(laptop, 1), "k".repeat(256)).andExpect(status().isBadRequest());

        assertThat(stockOf(laptop)).isEqualTo(10);
    }

    private ResultActions confirm(String body, String key) throws Exception {
        return mockMvc.perform(post("/cart/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", key)
                .content(body));
    }

    private static String cart(Product product, int qty) {
        return "{\"items\":[{\"productId\":\"%s\",\"qty\":%d}]}".formatted(product.getId(), qty);
    }

    private static String orderIdOf(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.orderId");
    }

    private int stockOf(Product product) {
        return products.findById(product.getId()).orElseThrow().getStock();
    }
}
