package com.promoquoter.product;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Test
    void reservingTakesUnitsOutOfStock() {
        Product product = product(5);

        product.reserve(3);
        product.reserve(2);

        assertThat(product.getStock()).isZero();
    }

    @Test
    void reportsWhetherEnoughStockIsLeft() {
        Product product = product(2);

        assertThat(product.hasStock(2)).isTrue();
        assertThat(product.hasStock(3)).isFalse();
    }

    @Test
    void refusesToReserveMoreThanIsInStock() {
        Product product = product(2);

        assertThatThrownBy(() -> product.reserve(3)).isInstanceOf(IllegalStateException.class);
        assertThat(product.getStock()).isEqualTo(2);
    }

    @Test
    void refusesToReserveANonPositiveQuantity() {
        assertThatThrownBy(() -> product(2).reserve(0)).isInstanceOf(IllegalArgumentException.class);
    }

    private static Product product(int stock) {
        return new Product("Pen", "office", new BigDecimal("1.00"), stock);
    }
}
