package com.promoquoter.pricing.rules;

import com.promoquoter.pricing.LineItem;
import com.promoquoter.pricing.Money;
import com.promoquoter.pricing.PricingContext;
import com.promoquoter.pricing.PromotionRule;
import com.promoquoter.pricing.PromotionRuleProvider;
import com.promoquoter.pricing.RulePriority;
import com.promoquoter.promotion.BuyXGetYPromotion;
import com.promoquoter.promotion.PromotionType;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * "Buy X get Y free" for one product. The free units must be in the cart: out of every complete
 * group of {@code X + Y} units, {@code Y} are free. Free units are valued at the line's current
 * (already discounted) unit price.
 *
 * <p>Example, buy 2 get 1: 3 units → 1 free, 5 units → 1 free, 6 units → 2 free.
 */
public record BuyXGetYRule(UUID promotionId, String name, UUID productId, int buyQuantity, int freeQuantity)
        implements PromotionRule {

    public BuyXGetYRule {
        if (buyQuantity <= 0 || freeQuantity <= 0) {
            throw new IllegalArgumentException("buyQuantity and freeQuantity must be positive");
        }
    }

    @Override
    public PromotionType type() {
        return PromotionType.BUY_X_GET_Y;
    }

    @Override
    public int priority() {
        return RulePriority.FREE_ITEMS;
    }

    @Override
    public void apply(PricingContext context) {
        context.line(productId).ifPresent(line -> {
            int freeUnits = freeUnits(line.quantity());
            if (freeUnits > 0) {
                line.applyDiscount(this, Money.proportion(line.total(), freeUnits, line.quantity()));
            }
        });
    }

    int freeUnits(int quantity) {
        return (quantity / (buyQuantity + freeQuantity)) * freeQuantity;
    }

    @Component
    public static class Provider implements PromotionRuleProvider<BuyXGetYPromotion> {

        @Override
        public Class<BuyXGetYPromotion> promotionClass() {
            return BuyXGetYPromotion.class;
        }

        @Override
        public PromotionRule create(BuyXGetYPromotion promotion) {
            return new BuyXGetYRule(promotion.getId(), promotion.getName(), promotion.getProductId(),
                    promotion.getBuyQuantity(), promotion.getFreeQuantity());
        }
    }
}
