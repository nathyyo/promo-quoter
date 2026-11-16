package com.promoquoter.pricing.rules;

import com.promoquoter.pricing.LineItem;
import com.promoquoter.pricing.Money;
import com.promoquoter.pricing.PricingContext;
import com.promoquoter.pricing.PromotionRule;
import com.promoquoter.pricing.PromotionRuleProvider;
import com.promoquoter.pricing.RulePriority;
import com.promoquoter.promotion.PercentOffCategoryPromotion;
import com.promoquoter.promotion.PromotionType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Takes {@code percent}% off the current total of every line in {@code category}.
 */
public record PercentOffCategoryRule(UUID promotionId, String name, String category, BigDecimal percent)
        implements PromotionRule {

    @Override
    public PromotionType type() {
        return PromotionType.PERCENT_OFF_CATEGORY;
    }

    @Override
    public int priority() {
        return RulePriority.CATEGORY_DISCOUNT;
    }

    @Override
    public void apply(PricingContext context) {
        for (LineItem line : context.lines()) {
            if (line.category().equals(category)) {
                line.applyDiscount(this, Money.percentOf(line.total(), percent));
            }
        }
    }

    @Component
    public static class Provider implements PromotionRuleProvider<PercentOffCategoryPromotion> {

        @Override
        public Class<PercentOffCategoryPromotion> promotionClass() {
            return PercentOffCategoryPromotion.class;
        }

        @Override
        public PromotionRule create(PercentOffCategoryPromotion promotion) {
            return new PercentOffCategoryRule(
                    promotion.getId(), promotion.getName(), promotion.getCategory(), promotion.getPercent());
        }
    }
}
