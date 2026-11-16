package com.promoquoter.pricing;

import com.promoquoter.promotion.PromotionType;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A promotion that reduced the cart total, with the amount it saved.
 */
public record AppliedPromotion(UUID promotionId, String name, PromotionType type, BigDecimal discount) {

    static AppliedPromotion from(AuditEntry entry) {
        return new AppliedPromotion(entry.promotionId(), entry.promotionName(), entry.type(), entry.discount());
    }
}
