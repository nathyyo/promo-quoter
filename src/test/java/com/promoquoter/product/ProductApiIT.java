package com.promoquoter.product;

import com.promoquoter.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductApiIT extends IntegrationTest {

    @Autowired
    private ProductRepository repository;

    @Test
    void createsABatchOfProducts() throws Exception {
        postJson("/products", List.of(
                product("Laptop", "electronics", "999.99", 10),
                product("Mouse", "Electronics", "25.00", 50)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", notNullValue()))
                .andExpect(jsonPath("$[0].name").value("Laptop"))
                .andExpect(jsonPath("$[0].category").value("ELECTRONICS"))
                .andExpect(jsonPath("$[0].price").value(999.99))
                .andExpect(jsonPath("$[0].stock").value(10))
                .andExpect(jsonPath("$[1].category").value("ELECTRONICS"));

        assertThat(repository.count()).isEqualTo(2);
    }

    @Test
    void acceptsASingleProductObject() throws Exception {
        postJson("/products", product("Book", "books", "12", 3))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Book"))
                .andExpect(content().string(containsString("\"price\":12.00")));
    }

    @Test
    void returnsAProductById() throws Exception {
        Product saved = repository.save(new Product("Desk", "furniture", new BigDecimal("150.00"), 4));

        mockMvc.perform(get("/products/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId().toString()))
                .andExpect(jsonPath("$.stock").value(4));
    }

    @Test
    void returns404ForAnUnknownProduct() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(get("/products/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.detail").value("Product with id '" + id + "' does not exist."));
    }

    @Test
    void returns400ForAMalformedId() throws Exception {
        mockMvc.perform(get("/products/{id}", "not-a-uuid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsInvalidFieldsWithOneEntryPerViolation() throws Exception {
        postJson("/products", List.of(
                product("Valid", "misc", "1.00", 1),
                product(" ", "misc", "-1.00", -5)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors", hasSize(3)))
                .andExpect(jsonPath("$.errors[0].field").value("[1].name"))
                .andExpect(jsonPath("$.errors[1].field").value("[1].price"))
                .andExpect(jsonPath("$.errors[2].field").value("[1].stock"));

        assertThat(repository.count()).as("nothing is stored when any product is invalid").isZero();
    }

    @Test
    void rejectsPricesWithMoreThanTwoDecimals() throws Exception {
        postJson("/products", product("Pen", "office", "1.999", 1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("[0].price"));
    }

    @Test
    void rejectsMissingRequiredFields() throws Exception {
        postJson("/products", "[{\"name\": \"Pen\"}]")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(3)));
    }

    @Test
    void rejectsAnEmptyBatch() throws Exception {
        postJson("/products", "[]")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("body"));
    }

    @Test
    void rejectsABatchAboveTheLimit() throws Exception {
        List<Map<String, Object>> tooMany = IntStream.rangeClosed(0, ProductController.MAX_BATCH_SIZE)
                .mapToObj(i -> product("P" + i, "misc", "1.00", 1))
                .toList();

        postJson("/products", tooMany).andExpect(status().isBadRequest());
    }

    @Test
    void rejectsUnknownFields() throws Exception {
        postJson("/products", "{\"name\":\"Pen\",\"category\":\"office\",\"price\":1,\"stock\":1,\"colour\":\"red\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
                .andExpect(jsonPath("$.detail").value("Unknown property 'colour' at '[0].colour'."));
    }

    @Test
    void rejectsMalformedJson() throws Exception {
        postJson("/products", "{not json")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
                .andExpect(jsonPath("$.detail").value("Request body is not valid JSON."));
    }

    static Map<String, Object> product(String name, String category, String price, int stock) {
        return Map.of("name", name, "category", category, "price", new BigDecimal(price), "stock", stock);
    }
}
