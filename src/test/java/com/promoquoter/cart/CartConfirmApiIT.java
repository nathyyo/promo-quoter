package com.promoquoter.cart;

import com.jayway.jsonpath.JsonPath;
import com.promoquoter.order.Order;
import com.promoquoter.order.OrderRepository;
import com.promoquoter.product.Product;
import com.promoquoter.product.ProductRepository;
import com.promoquoter.promotion.BuyXGetYPromotion;
import com.promoquoter.promotion.PercentOffCategoryPromotion;
import com.promoquoter.promotion.PromotionRepository;
import com.promoquoter.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.promoquoter.cart.CartQuoteApiIT.cart;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CartConfirmApiIT extends IntegrationTest {

    @Autowired
    private ProductRepository products;

    @Autowired
    private PromotionRepository promotions;

    @Autowired
    private OrderRepository orders;

    @Autowired
    private TransactionTemplate transactions;

    private Product laptop;
    private Product novel;

    @BeforeEach
    void seedCatalogue() {
        laptop = products.save(new Product("Laptop", "electronics", new BigDecimal("1000.00"), 10));
        novel = products.save(new Product("Novel", "books", new BigDecimal("15.99"), 2));
    }

    @Test
    void confirmsAnOrderReservesStockAndReturnsTheFinalPrice() throws Exception {
        promotions.save(new PercentOffCategoryPromotion("Electronics 10%", Set.of(), "electronics", BigDecimal.TEN));
        promotions.save(new BuyXGetYPromotion("Laptop B2G1", Set.of(), laptop.getId(), 2, 1));

        String response = postJson("/cart/confirm", cart(Map.of(laptop.getId(), 3), "REGULAR"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").isNotEmpty())
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.customerSegment").value("REGULAR"))
                .andExpect(jsonPath("$.lines[0].qty").value(3))
                .andExpect(jsonPath("$.appliedPromotions", hasSize(2)))
                .andExpect(jsonPath("$.subtotal").value(3000.00))
                .andExpect(jsonPath("$.discountTotal").value(1200.00))
                .andExpect(jsonPath("$.total").value(1800.00))
                .andReturn().getResponse().getContentAsString();

        assertThat(stockOf(laptop)).isEqualTo(7);

        UUID orderId = UUID.fromString(JsonPath.read(response, "$.orderId"));
        transactions.executeWithoutResult(status -> {
            Order order = orders.findById(orderId).orElseThrow();
            assertThat(order.getTotal()).isEqualByComparingTo("1800.00");
            assertThat(order.getDiscountTotal()).isEqualByComparingTo("1200.00");
            assertThat(order.getLines()).singleElement().satisfies(line -> {
                assertThat(line.getProductId()).isEqualTo(laptop.getId());
                assertThat(line.getQuantity()).isEqualTo(3);
                assertThat(line.getTotal()).isEqualByComparingTo("1800.00");
            });
        });
    }

    @Test
    void confirmedPriceMatchesTheQuote() throws Exception {
        promotions.save(new PercentOffCategoryPromotion("Books 15%", Set.of(), "books", BigDecimal.valueOf(15)));
        Map<String, Object> cart = cart(Map.of(novel.getId(), 2), "REGULAR");

        String quote = postJson("/cart/quote", cart).andReturn().getResponse().getContentAsString();

        postJson("/cart/confirm", cart)
                .andExpect(status().isCreated())
                .andExpect(content().json(quote, false));
    }

    @Test
    void allowsTakingTheLastUnit() throws Exception {
        postJson("/cart/confirm", cart(Map.of(novel.getId(), 2), "REGULAR")).andExpect(status().isCreated());

        assertThat(stockOf(novel)).isZero();
    }

    @Test
    void returns409WhenStockRunsOutAndReservesNothing() throws Exception {
        String body = """
                {"items": [{"productId": "%s", "qty": 1}, {"productId": "%s", "qty": 3}]}
                """.formatted(laptop.getId(), novel.getId());

        postJson("/cart/confirm", body)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("OUT_OF_STOCK"))
                .andExpect(jsonPath("$.title").value("Insufficient stock"))
                .andExpect(jsonPath("$.detail").value("Not enough stock for 'Novel': requested 3, available 2."))
                .andExpect(jsonPath("$.shortages", hasSize(1)))
                .andExpect(jsonPath("$.shortages[0].productId").value(novel.getId().toString()))
                .andExpect(jsonPath("$.shortages[0].requested").value(3))
                .andExpect(jsonPath("$.shortages[0].available").value(2));

        assertThat(stockOf(laptop)).as("no partial reservation").isEqualTo(10);
        assertThat(stockOf(novel)).isEqualTo(2);
        assertThat(orders.count()).isZero();
    }

    @Test
    void reportsEveryShortageAtOnce() throws Exception {
        String body = """
                {"items": [{"productId": "%s", "qty": 11}, {"productId": "%s", "qty": 3}]}
                """.formatted(laptop.getId(), novel.getId());

        postJson("/cart/confirm", body)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Not enough stock for 2 products."))
                .andExpect(jsonPath("$.shortages", hasSize(2)));
    }

    @Test
    void checksTheCombinedQuantityOfRepeatedLines() throws Exception {
        String body = """
                {"items": [{"productId": "%1$s", "qty": 1}, {"productId": "%1$s", "qty": 2}]}
                """.formatted(novel.getId());

        postJson("/cart/confirm", body)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.shortages[0].requested").value(3));
    }

    @Test
    void rejectsUnknownProductsWithoutReserving() throws Exception {
        String body = """
                {"items": [{"productId": "%s", "qty": 1}, {"productId": "%s", "qty": 1}]}
                """.formatted(laptop.getId(), UUID.randomUUID());

        postJson("/cart/confirm", body)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("UNKNOWN_PRODUCT"));

        assertThat(stockOf(laptop)).isEqualTo(10);
    }

    @Test
    void validatesTheCartLikeAQuote() throws Exception {
        postJson("/cart/confirm", "{\"items\":[{\"productId\":\"%s\",\"qty\":-1}]}".formatted(laptop.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("items[0].qty"));
    }

    private int stockOf(Product product) {
        return products.findById(product.getId()).orElseThrow().getStock();
    }
}
