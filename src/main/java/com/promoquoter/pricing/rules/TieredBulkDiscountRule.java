package com.promoquoter.pricing.rules;

import com.promoquoter.pricing.LineItem;
import com.promoquoter.pricing.Money;
import com.promoquoter.pricing.PricingContext;
import com.promoquoter.pricing.PromotionRule;
import com.promoquoter.pricing.PromotionRuleProvider;
import com.promoquoter.pricing.RulePriority;
import com.promoquoter.promotion.PromotionType;
import com.promoquoter.promotion.TieredBulkDiscountPromotion;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Takes a percentage off one product's line once its quantity reaches a tier threshold. Only the
 * highest tier reached applies; tiers do not stack.
 */
public record TieredBulkDiscountRule(UUID promotionId, String name, UUID productId, List<Tier> tiers)
        implements PromotionRule {

    public record Tier(int minQuantity, BigDecimal percent) {
    }

    public TieredBulkDiscountRule {
        tiers = tiers.stream().sorted(Comparator.comparingInt(Tier::minQuantity)).toList();
    }

    @Override
    public PromotionType type() {
        return PromotionType.TIERED_BULK_DISCOUNT;
    }

    @Override
    public int priority() {
        return RulePriority.VOLUME_DISCOUNT;
    }

    @Override
    public void apply(PricingContext context) {
        context.line(productId).ifPresent(line ->
                tierFor(line.quantity()).ifPresent(tier ->
                        line.applyDiscount(this, Money.percentOf(line.total(), tier.percent()))));
    }

    Optional<Tier> tierFor(int quantity) {
        return tiers.stream()
                .filter(tier -> quantity >= tier.minQuantity())
                .reduce((lower, higher) -> higher);
    }

    @Component
    public static class Provider implements PromotionRuleProvider<TieredBulkDiscountPromotion> {

        @Override
        public Class<TieredBulkDiscountPromotion> promotionClass() {
            return TieredBulkDiscountPromotion.class;
        }

        @Override
        public PromotionRule create(TieredBulkDiscountPromotion promotion) {
            List<Tier> tiers = promotion.getTiers().stream()
                    .map(tier -> new Tier(tier.getMinQuantity(), tier.getPercent()))
                    .toList();
            return new TieredBulkDiscountRule(promotion.getId(), promotion.getName(), promotion.getProductId(), tiers);
        }
    }
}
