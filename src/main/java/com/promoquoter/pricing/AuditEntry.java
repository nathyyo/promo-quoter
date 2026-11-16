package com.promoquoter.pricing;

import com.promoquoter.promotion.PromotionType;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * One step of a price calculation: which promotion ran and how it changed the cart total.
 */
public record AuditEntry(
        int step,
        UUID promotionId,
        String promotionName,
        PromotionType type,
        BigDecimal totalBefore,
        BigDecimal totalAfter) {

    public BigDecimal discount() {
        return totalBefore.subtract(totalAfter);
    }

    public boolean changedPrice() {
        return discount().signum() != 0;
    }
}
