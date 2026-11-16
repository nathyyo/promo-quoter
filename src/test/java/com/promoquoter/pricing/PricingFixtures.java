package com.promoquoter.pricing;

import com.promoquoter.pricing.rules.BuyXGetYRule;
import com.promoquoter.pricing.rules.PercentOffCategoryRule;
import com.promoquoter.pricing.rules.TieredBulkDiscountRule;
import com.promoquoter.promotion.CustomerSegment;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Builders shared by the pricing unit tests.
 */
public final class PricingFixtures {

    private PricingFixtures() {
    }

    public static PricingItem item(String name, String category, String unitPrice, int quantity) {
        return new PricingItem(UUID.randomUUID(), name, category, new BigDecimal(unitPrice), quantity);
    }

    public static PricingContext context(PricingItem... items) {
        return new PricingContext(CustomerSegment.REGULAR, List.of(items));
    }

    public static BigDecimal money(String amount) {
        return new BigDecimal(amount);
    }

    public static PromotionRuleFactory defaultRuleFactory() {
        return new PromotionRuleFactory(List.of(
                new PercentOffCategoryRule.Provider(),
                new TieredBulkDiscountRule.Provider(),
                new BuyXGetYRule.Provider()));
    }
}
