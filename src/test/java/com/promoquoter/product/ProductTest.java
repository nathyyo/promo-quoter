package com.promoquoter.product;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ProductTest {

    @Test
    void normalizesCategoryToUpperCaseAndTrimsName() {
        Product product = new Product("  Laptop ", " electronics ", new BigDecimal("10.00"), 1);

        assertThat(product.getName()).isEqualTo("Laptop");
        assertThat(product.getCategory()).isEqualTo("ELECTRONICS");
        assertThat(product.getId()).isNotNull();
    }

    @Test
    void normalizeCategoryIsCaseInsensitive() {
        assertThat(Product.normalizeCategory("Books")).isEqualTo(Product.normalizeCategory("BOOKS"));
    }
}
