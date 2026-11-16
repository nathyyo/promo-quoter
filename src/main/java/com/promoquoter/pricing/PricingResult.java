package com.promoquoter.pricing;

import com.promoquoter.promotion.CustomerSegment;

import java.math.BigDecimal;
import java.util.List;

/**
 * The outcome of pricing a cart.
 *
 * @param appliedPromotions promotions that reduced the total, in the order they were applied
 * @param auditTrail        every promotion that was evaluated, including those that changed nothing
 */
public record PricingResult(
        CustomerSegment customerSegment,
        List<PricedLine> lines,
        List<AppliedPromotion> appliedPromotions,
        BigDecimal subtotal,
        BigDecimal discountTotal,
        BigDecimal total,
        List<AuditEntry> auditTrail) {
}
