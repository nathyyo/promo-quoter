package com.promoquoter.pricing;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * One cart line handed to the pricing engine: a snapshot of the product plus the requested quantity.
 */
public record PricingItem(UUID productId, String name, String category, BigDecimal unitPrice, int quantity) {

    public PricingItem {
        Objects.requireNonNull(productId, "productId");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(unitPrice, "unitPrice");
        if (unitPrice.signum() < 0) {
            throw new IllegalArgumentException("unitPrice must not be negative");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
    }
}
