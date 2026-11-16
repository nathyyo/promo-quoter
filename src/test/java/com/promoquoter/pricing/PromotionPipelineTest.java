package com.promoquoter.pricing;

import com.promoquoter.promotion.PromotionType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.promoquoter.pricing.PricingFixtures.context;
import static com.promoquoter.pricing.PricingFixtures.item;
import static com.promoquoter.pricing.PricingFixtures.money;
import static org.assertj.core.api.Assertions.assertThat;

class PromotionPipelineTest {

    private final List<String> executionOrder = new ArrayList<>();

    @Test
    void runsRulesByPriorityRegardlessOfInputOrder() {
        PromotionPipeline pipeline = new PromotionPipeline(List.of(
                recording("free-items", RulePriority.FREE_ITEMS),
                recording("category", RulePriority.CATEGORY_DISCOUNT),
                recording("volume", RulePriority.VOLUME_DISCOUNT)));

        pipeline.run(context(item("A", "MISC", "1.00", 1)));

        assertThat(executionOrder).containsExactly("category", "volume", "free-items");
    }

    @Test
    void keepsInputOrderForRulesWithEqualPriority() {
        PromotionPipeline pipeline = new PromotionPipeline(List.of(
                recording("first", RulePriority.CATEGORY_DISCOUNT),
                recording("second", RulePriority.CATEGORY_DISCOUNT),
                recording("third", RulePriority.CATEGORY_DISCOUNT)));

        pipeline.run(context(item("A", "MISC", "1.00", 1)));

        assertThat(executionOrder).containsExactly("first", "second", "third");
    }

    @Test
    void acceptsNewRuleTypesWithoutChanges() {
        PromotionRule flatAmountOff = new TestRule("flat", 50) {
            @Override
            public void apply(PricingContext context) {
                context.lines().forEach(line -> line.applyDiscount(this, money("1.00")));
            }
        };
        PricingContext context = context(item("A", "MISC", "5.00", 1));

        new PromotionPipeline(List.of(flatAmountOff)).run(context);

        assertThat(context.currentTotal()).isEqualTo(money("4.00"));
    }

    @Test
    void anEmptyPipelineLeavesPricesUnchanged() {
        PricingContext context = context(item("A", "MISC", "5.00", 2));

        new PromotionPipeline(List.of()).run(context);

        assertThat(context.currentTotal()).isEqualTo(money("10.00"));
    }

    private PromotionRule recording(String name, int priority) {
        return new TestRule(name, priority) {
            @Override
            public void apply(PricingContext context) {
                executionOrder.add(name);
            }
        };
    }

    private abstract static class TestRule implements PromotionRule {

        private final UUID id = UUID.randomUUID();
        private final String name;
        private final int priority;

        TestRule(String name, int priority) {
            this.name = name;
            this.priority = priority;
        }

        @Override
        public UUID promotionId() {
            return id;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public PromotionType type() {
            return PromotionType.PERCENT_OFF_CATEGORY;
        }

        @Override
        public int priority() {
            return priority;
        }
    }
}
