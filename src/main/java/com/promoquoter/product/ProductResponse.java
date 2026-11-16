package com.promoquoter.product;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductResponse(UUID id, String name, String category, BigDecimal price, int stock) {

    static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getCategory(),
                product.getPrice(),
                product.getStock());
    }
}
