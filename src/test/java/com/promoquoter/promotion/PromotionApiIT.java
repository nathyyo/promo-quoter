package com.promoquoter.promotion;

import com.promoquoter.product.Product;
import com.promoquoter.product.ProductRepository;
import com.promoquoter.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PromotionApiIT extends IntegrationTest {

    @Autowired
    private ProductRepository products;

    @Autowired
    private PromotionRepository promotions;

    private UUID laptopId;

    @BeforeEach
    void createProduct() {
        laptopId = products.save(new Product("Laptop", "electronics", new BigDecimal("1000.00"), 10)).getId();
    }

    @Test
    void createsAPercentOffCategoryPromotion() throws Exception {
        postJson("/promotions", percentOff("Electronics sale", "electronics", "10"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").isNotEmpty())
                .andExpect(jsonPath("$[0].type").value("PERCENT_OFF_CATEGORY"))
                .andExpect(jsonPath("$[0].name").value("Electronics sale"))
                .andExpect(jsonPath("$[0].category").value("ELECTRONICS"))
                .andExpect(jsonPath("$[0].percent").value(10))
                .andExpect(jsonPath("$[0].customerSegments").isEmpty())
                .andExpect(jsonPath("$[0].productId").doesNotExist())
                .andExpect(jsonPath("$[0].tiers").doesNotExist());
    }

    @Test
    void createsABuyXGetYPromotion() throws Exception {
        postJson("/promotions", buyXGetY(laptopId, 2, 1))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].type").value("BUY_X_GET_Y"))
                .andExpect(jsonPath("$[0].productId").value(laptopId.toString()))
                .andExpect(jsonPath("$[0].buyQuantity").value(2))
                .andExpect(jsonPath("$[0].freeQuantity").value(1))
                .andExpect(jsonPath("$[0].category").doesNotExist());
    }

    @Test
    void createsATieredBulkDiscountWithTiersSortedByThreshold() throws Exception {
        postJson("/promotions", tiered(laptopId, List.of(tier(20, "10"), tier(10, "5"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].type").value("TIERED_BULK_DISCOUNT"))
                .andExpect(jsonPath("$[0].productId").value(laptopId.toString()))
                .andExpect(jsonPath("$[0].tiers[0].minQuantity").value(10))
                .andExpect(jsonPath("$[0].tiers[0].percent").value(5))
                .andExpect(jsonPath("$[0].tiers[1].minQuantity").value(20));
    }

    @Test
    void createsAMixedBatchRestrictedToSegments() throws Exception {
        Map<String, Object> vipOnly = percentOff("VIP sale", "electronics", "15");
        vipOnly.put("customerSegments", List.of("VIP", "PREMIUM"));

        postJson("/promotions", List.of(vipOnly, buyXGetY(laptopId, 3, 1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].customerSegments", contains("PREMIUM", "VIP")))
                .andExpect(jsonPath("$[1].type").value("BUY_X_GET_Y"));

        assertThat(promotions.count()).isEqualTo(2);
    }

    @Test
    void listsPromotionsInCreationOrder() throws Exception {
        postJson("/promotions", percentOff("First", "books", "5")).andExpect(status().isCreated());
        postJson("/promotions", buyXGetY(laptopId, 1, 1)).andExpect(status().isCreated());

        mockMvc.perform(get("/promotions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name").value("First"))
                .andExpect(jsonPath("$[1].type").value("BUY_X_GET_Y"));
    }

    @Test
    void rejectsPromotionsForUnknownProducts() throws Exception {
        UUID unknown = UUID.randomUUID();

        postJson("/promotions", List.of(percentOff("Sale", "books", "5"), buyXGetY(unknown, 2, 1)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("UNKNOWN_PRODUCT"))
                .andExpect(jsonPath("$.detail").value(containsString(unknown.toString())));

        assertThat(promotions.count()).as("the whole batch is rejected").isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-5", "100.01", "12.345"})
    void rejectsOutOfRangePercentages(String percent) throws Exception {
        postJson("/promotions", percentOff("Sale", "books", percent))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("[0].percent"));
    }

    @Test
    void acceptsAHundredPercentDiscount() throws Exception {
        postJson("/promotions", percentOff("Free books", "books", "100")).andExpect(status().isCreated());
    }

    @Test
    void rejectsNonPositiveBuyXGetYQuantities() throws Exception {
        postJson("/promotions", buyXGetY(laptopId, 0, -1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(2)))
                .andExpect(jsonPath("$.errors[0].field").value("[0].buyQuantity"))
                .andExpect(jsonPath("$.errors[1].field").value("[0].freeQuantity"));
    }

    @Test
    void rejectsTiersWithDuplicateThresholds() throws Exception {
        postJson("/promotions", tiered(laptopId, List.of(tier(10, "5"), tier(10, "8"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("[0].tierThresholdsUnique"))
                .andExpect(jsonPath("$.errors[0].message").value("each tier must have a different minQuantity"));
    }

    @Test
    void rejectsInvalidTiers() throws Exception {
        postJson("/promotions", tiered(laptopId, List.of(tier(0, "150"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(2)))
                .andExpect(jsonPath("$.errors[0].field").value("[0].tiers[0].minQuantity"))
                .andExpect(jsonPath("$.errors[1].field").value("[0].tiers[0].percent"));
    }

    @Test
    void rejectsATieredDiscountWithoutTiers() throws Exception {
        postJson("/promotions", tiered(laptopId, List.of()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("[0].tiers"));
    }

    @Test
    void rejectsAnUnknownPromotionType() throws Exception {
        postJson("/promotions", "{\"type\":\"FREE_LUNCH\",\"name\":\"x\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
                .andExpect(jsonPath("$.detail").value(containsString("Unknown type 'FREE_LUNCH'")));
    }

    @Test
    void rejectsAPromotionWithoutAType() throws Exception {
        postJson("/promotions", "{\"name\":\"x\",\"category\":\"books\",\"percent\":5}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("Missing 'type' property")));
    }

    @Test
    void rejectsAnUnknownCustomerSegment() throws Exception {
        Map<String, Object> body = percentOff("Sale", "books", "5");
        body.put("customerSegments", List.of("GOLD"));

        postJson("/promotions", body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("Invalid value 'GOLD'")))
                .andExpect(jsonPath("$.detail").value(containsString("[REGULAR, PREMIUM, VIP]")));
    }

    @Test
    void rejectsAnEmptyBatch() throws Exception {
        postJson("/promotions", "[]")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    static Map<String, Object> percentOff(String name, String category, String percent) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "PERCENT_OFF_CATEGORY");
        body.put("name", name);
        body.put("category", category);
        body.put("percent", new BigDecimal(percent));
        return body;
    }

    static Map<String, Object> buyXGetY(UUID productId, int buy, int free) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "BUY_X_GET_Y");
        body.put("name", "Buy " + buy + " get " + free);
        body.put("productId", productId);
        body.put("buyQuantity", buy);
        body.put("freeQuantity", free);
        return body;
    }

    static Map<String, Object> tiered(UUID productId, List<Map<String, Object>> tiers) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "TIERED_BULK_DISCOUNT");
        body.put("name", "Bulk discount");
        body.put("productId", productId);
        body.put("tiers", tiers);
        return body;
    }

    static Map<String, Object> tier(int minQuantity, String percent) {
        return Map.of("minQuantity", minQuantity, "percent", new BigDecimal(percent));
    }
}
