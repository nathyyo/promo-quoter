package com.promoquoter.pricing;

import com.promoquoter.promotion.Promotion;

/**
 * Builds the rule for one kind of promotion definition. Register an implementation as a Spring bean
 * and {@link PromotionRuleFactory} picks it up; nothing else needs to change.
 *
 * @param <P> the promotion definition this provider understands
 */
public interface PromotionRuleProvider<P extends Promotion> {

    Class<P> promotionClass();

    PromotionRule create(P promotion);
}
