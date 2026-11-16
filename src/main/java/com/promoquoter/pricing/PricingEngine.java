package com.promoquoter.pricing;

import com.promoquoter.promotion.CustomerSegment;
import com.promoquoter.promotion.Promotion;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Prices a cart. Promotions are filtered by customer segment, turned into rules by the
 * {@link PromotionRuleFactory}, wrapped for auditing, and applied by a {@link PromotionPipeline}.
 *
 * <p>The engine has no persistence or web dependencies, so callers decide where products and
 * promotions come from.
 */
@Component
public class PricingEngine {

    private final PromotionRuleFactory ruleFactory;

    public PricingEngine(PromotionRuleFactory ruleFactory) {
        this.ruleFactory = ruleFactory;
    }

    /**
     * @param items      cart lines, at most one per product
     * @param segment    the customer's segment; promotions limited to other segments are skipped
     * @param promotions candidate promotions, in creation order (this breaks ties within a priority)
     */
    public PricingResult price(List<PricingItem> items, CustomerSegment segment, List<? extends Promotion> promotions) {
        List<PromotionRule> rules = promotions.stream()
                .filter(promotion -> promotion.appliesTo(segment))
                .map(ruleFactory::create)
                .<PromotionRule>map(AuditingPromotionRule::new)
                .toList();

        PricingContext context = new PricingContext(segment, items);
        new PromotionPipeline(rules).run(context);
        return context.toResult();
    }
}
