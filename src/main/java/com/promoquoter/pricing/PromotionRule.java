package com.promoquoter.pricing;

import com.promoquoter.promotion.PromotionType;

import java.util.UUID;

/**
 * Strategy for one promotion. Each implementation contains its own discount algorithm and decides
 * which cart lines it applies to; the {@link PromotionPipeline} only decides the order.
 */
public interface PromotionRule {

    UUID promotionId();

    String name();

    PromotionType type();

    /**
     * Position in the pipeline: lower values run first. Rules with the same priority keep their input order.
     *
     * @see RulePriority
     */
    int priority();

    /** Applies this promotion's discounts to the lines in the context. */
    void apply(PricingContext context);
}
