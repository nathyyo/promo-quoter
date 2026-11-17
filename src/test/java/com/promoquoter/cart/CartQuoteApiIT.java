package com.promoquoter.cart;

import com.promoquoter.product.Product;
import com.promoquoter.product.ProductRepository;
import com.promoquoter.promotion.BuyXGetYPromotion;
import com.promoquoter.promotion.CustomerSegment;
import com.promoquoter.promotion.DiscountTier;
import com.promoquoter.promotion.PercentOffCategoryPromotion;
import com.promoquoter.promotion.PromotionRepository;
import com.promoquoter.promotion.TieredBulkDiscountPromotion;
import com.promoquoter.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CartQuoteApiIT extends IntegrationTest {

    @Autowired
    private ProductRepository products;

    @Autowired
    private PromotionRepository promotions;

    private Product laptop;
    private Product mouse;
    private Product novel;

    @BeforeEach
    void seedCatalogue() {
        laptop = products.save(new Product("Laptop", "electronics", new BigDecimal("1000.00"), 10));
        mouse = products.save(new Product("Mouse", "electronics", new BigDecimal("25.00"), 100));
        novel = products.save(new Product("Novel", "books", new BigDecimal("15.99"), 5));
    }

    @Test
    void quotesACartWithoutPromotions() throws Exception {
        postJson("/cart/quote", cart(Map.of(novel.getId(), 2), "REGULAR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerSegment").value("REGULAR"))
                .andExpect(jsonPath("$.lines", hasSize(1)))
                .andExpect(jsonPath("$.lines[0].productId").value(novel.getId().toString()))
                .andExpect(jsonPath("$.lines[0].name").value("Novel"))
                .andExpect(jsonPath("$.lines[0].unitPrice").value(15.99))
                .andExpect(jsonPath("$.lines[0].qty").value(2))
                .andExpect(jsonPath("$.lines[0].subtotal").value(31.98))
                .andExpect(jsonPath("$.lines[0].discounts").isEmpty())
                .andExpect(jsonPath("$.lines[0].total").value(31.98))
                .andExpect(jsonPath("$.appliedPromotions").isEmpty())
                .andExpect(jsonPath("$.subtotal").value(31.98))
                .andExpect(jsonPath("$.discountTotal").value(0.00))
                .andExpect(jsonPath("$.total").value(31.98));
    }

    @Test
    void itemisesStackedPromotionsInApplicationOrder() throws Exception {
        BuyXGetYPromotion laptopDeal = promotions.save(
                new BuyXGetYPromotion("Laptop buy 2 get 1", Set.of(), laptop.getId(), 2, 1));
        TieredBulkDiscountPromotion mouseBulk = promotions.save(new TieredBulkDiscountPromotion(
                "Mouse bulk", Set.of(), mouse.getId(), List.of(new DiscountTier(10, BigDecimal.valueOf(5)))));
        PercentOffCategoryPromotion electronics = promotions.save(
                new PercentOffCategoryPromotion("Electronics 10%", Set.of(), "electronics", BigDecimal.TEN));

        String body = """
                {"items": [
                  {"productId": "%s", "qty": 3},
                  {"productId": "%s", "qty": 12},
                  {"productId": "%s", "qty": 1}
                ], "customerSegment": "REGULAR"}
                """.formatted(laptop.getId(), mouse.getId(), novel.getId());

        postJson("/cart/quote", body)
                .andExpect(status().isOk())
                // Laptop: 3000.00 - 300.00 (10%) - 900.00 (1 free) = 1800.00
                .andExpect(jsonPath("$.lines[0].subtotal").value(3000.00))
                .andExpect(jsonPath("$.lines[0].discounts", hasSize(2)))
                .andExpect(jsonPath("$.lines[0].discounts[0].promotionName").value("Electronics 10%"))
                .andExpect(jsonPath("$.lines[0].discounts[0].amount").value(300.00))
                .andExpect(jsonPath("$.lines[0].discounts[1].promotionName").value("Laptop buy 2 get 1"))
                .andExpect(jsonPath("$.lines[0].discounts[1].amount").value(900.00))
                .andExpect(jsonPath("$.lines[0].discountTotal").value(1200.00))
                .andExpect(jsonPath("$.lines[0].total").value(1800.00))
                // Mouse: 300.00 - 30.00 (10%) - 13.50 (5% bulk) = 256.50
                .andExpect(jsonPath("$.lines[1].total").value(256.50))
                .andExpect(jsonPath("$.lines[2].total").value(15.99))
                .andExpect(jsonPath("$.appliedPromotions", hasSize(3)))
                .andExpect(jsonPath("$.appliedPromotions[0].order").value(1))
                .andExpect(jsonPath("$.appliedPromotions[0].promotionId").value(electronics.getId().toString()))
                .andExpect(jsonPath("$.appliedPromotions[0].type").value("PERCENT_OFF_CATEGORY"))
                .andExpect(jsonPath("$.appliedPromotions[0].discount").value(330.00))
                .andExpect(jsonPath("$.appliedPromotions[1].promotionId").value(mouseBulk.getId().toString()))
                .andExpect(jsonPath("$.appliedPromotions[1].discount").value(13.50))
                .andExpect(jsonPath("$.appliedPromotions[2].promotionId").value(laptopDeal.getId().toString()))
                .andExpect(jsonPath("$.appliedPromotions[2].order").value(3))
                .andExpect(jsonPath("$.subtotal").value(3315.99))
                .andExpect(jsonPath("$.discountTotal").value(1243.50))
                .andExpect(jsonPath("$.total").value(2072.49));
    }

    @Test
    void appliesSegmentRestrictedPromotionsOnlyToThatSegment() throws Exception {
        promotions.save(new PercentOffCategoryPromotion("VIP books", Set.of(CustomerSegment.VIP), "books",
                BigDecimal.valueOf(20)));

        postJson("/cart/quote", cart(Map.of(novel.getId(), 1), "REGULAR"))
                .andExpect(jsonPath("$.total").value(15.99));
        postJson("/cart/quote", cart(Map.of(novel.getId(), 1), "VIP"))
                .andExpect(jsonPath("$.customerSegment").value("VIP"))
                .andExpect(jsonPath("$.total").value(12.79));
    }

    @Test
    void defaultsToTheRegularSegment() throws Exception {
        String body = "{\"items\":[{\"productId\":\"%s\",\"qty\":1}]}".formatted(novel.getId());

        postJson("/cart/quote", body)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerSegment").value("REGULAR"));
    }

    @Test
    void combinesRepeatedLinesForTheSameProduct() throws Exception {
        promotions.save(new BuyXGetYPromotion("B2G1", Set.of(), laptop.getId(), 2, 1));
        String body = """
                {"items": [{"productId": "%1$s", "qty": 2}, {"productId": "%1$s", "qty": 1}]}
                """.formatted(laptop.getId());

        postJson("/cart/quote", body)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines", hasSize(1)))
                .andExpect(jsonPath("$.lines[0].qty").value(3))
                .andExpect(jsonPath("$.total").value(2000.00));
    }

    @Test
    void quotingDoesNotReserveStock() throws Exception {
        postJson("/cart/quote", cart(Map.of(laptop.getId(), 4), "REGULAR")).andExpect(status().isOk());

        assertThat(products.findById(laptop.getId())).get().extracting(Product::getStock).isEqualTo(10);
    }

    @Test
    void rejectsUnknownProducts() throws Exception {
        UUID unknown = UUID.randomUUID();

        postJson("/cart/quote", cart(Map.of(unknown, 1), "REGULAR"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("UNKNOWN_PRODUCT"))
                .andExpect(jsonPath("$.detail").value(containsString(unknown.toString())));
    }

    @Test
    void rejectsInvalidItems() throws Exception {
        String body = "{\"items\":[{\"productId\":null,\"qty\":0},{\"productId\":\"%s\",\"qty\":10001}]}"
                .formatted(novel.getId());

        postJson("/cart/quote", body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors", hasSize(3)))
                .andExpect(jsonPath("$.errors[0].field").value("items[0].productId"))
                .andExpect(jsonPath("$.errors[1].field").value("items[0].qty"))
                .andExpect(jsonPath("$.errors[2].field").value("items[1].qty"));
    }

    @Test
    void rejectsAnEmptyCart() throws Exception {
        postJson("/cart/quote", "{\"items\":[]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("items"));
    }

    @Test
    void rejectsAnUnknownSegment() throws Exception {
        String body = "{\"items\":[{\"productId\":\"%s\",\"qty\":1}],\"customerSegment\":\"GOLD\"}"
                .formatted(novel.getId());

        postJson("/cart/quote", body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
                .andExpect(jsonPath("$.detail").value(containsString("customerSegment")));
    }

    @Test
    void explainsValuesOfTheWrongType() throws Exception {
        postJson("/cart/quote", "{\"items\":[{\"productId\":\"%s\",\"qty\":\"three\"}]}".formatted(novel.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Invalid value 'three' at 'items[0].qty'."));
        postJson("/cart/quote", "{\"items\":\"not-a-list\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Unexpected value at 'items[0]'."));
    }

    @Test
    void reportsNumbersThatAreOutOfRange() throws Exception {
        postJson("/cart/quote", "{\"items\":[{\"productId\":\"%s\",\"qty\":99999999999}]}".formatted(novel.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Number out of range at 'items[0].qty'."));
    }

    @Test
    void rejectsAMissingBody() throws Exception {
        postJson("/cart/quote", "")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
                .andExpect(jsonPath("$.detail").value("Request body is missing or unreadable."));
    }

    static Map<String, Object> cart(Map<UUID, Integer> quantities, String segment) {
        List<Map<String, Object>> items = quantities.entrySet().stream()
                .map(e -> Map.<String, Object>of("productId", e.getKey(), "qty", e.getValue()))
                .toList();
        return Map.of("items", items, "customerSegment", segment);
    }
}
