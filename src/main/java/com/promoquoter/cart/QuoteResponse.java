package com.promoquoter.cart;

import com.promoquoter.pricing.AppliedPromotion;
import com.promoquoter.pricing.LineDiscount;
import com.promoquoter.pricing.PricedLine;
import com.promoquoter.pricing.PricingResult;
import com.promoquoter.promotion.CustomerSegment;
import com.promoquoter.promotion.PromotionType;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

/**
 * Itemised price of a cart.
 *
 * @param appliedPromotions promotions that reduced the price, in the order they were applied
 */
public record QuoteResponse(
        CustomerSegment customerSegment,
        List<Line> lines,
        List<Applied> appliedPromotions,
        BigDecimal subtotal,
        BigDecimal discountTotal,
        BigDecimal total) {

    public record Line(
            UUID productId,
            String name,
            String category,
            BigDecimal unitPrice,
            int qty,
            BigDecimal subtotal,
            List<Discount> discounts,
            BigDecimal discountTotal,
            BigDecimal total) {

        static Line from(PricedLine line) {
            return new Line(line.productId(), line.name(), line.category(), line.unitPrice(), line.quantity(),
                    line.subtotal(), line.discounts().stream().map(Discount::from).toList(),
                    line.discountTotal(), line.total());
        }
    }

    public record Discount(UUID promotionId, String promotionName, BigDecimal amount) {

        static Discount from(LineDiscount discount) {
            return new Discount(discount.promotionId(), discount.promotionName(), discount.amount());
        }
    }

    /**
     * @param order 1-based position in which the promotion was applied
     */
    public record Applied(int order, UUID promotionId, String name, PromotionType type, BigDecimal discount) {
    }

    public static QuoteResponse from(PricingResult result) {
        List<AppliedPromotion> applied = result.appliedPromotions();
        List<Applied> ordered = IntStream.range(0, applied.size())
                .mapToObj(i -> {
                    AppliedPromotion promotion = applied.get(i);
                    return new Applied(i + 1, promotion.promotionId(), promotion.name(), promotion.type(),
                            promotion.discount());
                })
                .toList();
        return new QuoteResponse(result.customerSegment(), result.lines().stream().map(Line::from).toList(),
                ordered, result.subtotal(), result.discountTotal(), result.total());
    }
}
