package com.promoquoter.pricing;

import java.util.Comparator;
import java.util.List;

/**
 * Applies rules one after another in priority order. Each rule sees the cart as left by the rules
 * before it. The pipeline knows nothing about specific promotion types, so rules can be added,
 * removed or replaced without changing this class.
 */
public final class PromotionPipeline {

    private final List<PromotionRule> rules;

    public PromotionPipeline(List<? extends PromotionRule> rules) {
        // Sorting an ordered stream is stable, so rules with equal priority keep their input order.
        this.rules = rules.stream()
                .sorted(Comparator.comparingInt(PromotionRule::priority))
                .map(PromotionRule.class::cast)
                .toList();
    }

    public void run(PricingContext context) {
        for (PromotionRule rule : rules) {
            rule.apply(context);
        }
    }

    /** The rules in the order they run. */
    public List<PromotionRule> rules() {
        return rules;
    }
}
