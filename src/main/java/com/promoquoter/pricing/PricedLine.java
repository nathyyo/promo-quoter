package com.promoquoter.pricing;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * The final price of one cart line: {@code total = subtotal - discountTotal}.
 */
public record PricedLine(
        UUID productId,
        String name,
        String category,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal subtotal,
        List<LineDiscount> discounts,
        BigDecimal discountTotal,
        BigDecimal total) {
}
