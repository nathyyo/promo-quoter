package com.promoquoter.pricing.rules;

import com.promoquoter.pricing.LineItem;
import com.promoquoter.pricing.PricingContext;
import com.promoquoter.pricing.RulePriority;
import com.promoquoter.promotion.PercentOffCategoryPromotion;
import com.promoquoter.promotion.PromotionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import static com.promoquoter.pricing.PricingFixtures.context;
import static com.promoquoter.pricing.PricingFixtures.item;
import static com.promoquoter.pricing.PricingFixtures.money;
import static org.assertj.core.api.Assertions.assertThat;

class PercentOffCategoryRuleTest {

    private final PercentOffCategoryRule rule =
            new PercentOffCategoryRule(UUID.randomUUID(), "Electronics 10%", "ELECTRONICS", BigDecimal.TEN);

    @Test
    void discountsEveryLineInTheCategory() {
        PricingContext context = context(
                item("Laptop", "ELECTRONICS", "999.99", 1),
                item("Mouse", "ELECTRONICS", "25.00", 2),
                item("Novel", "BOOKS", "12.00", 1));

        rule.apply(context);

        assertThat(context.lines()).extracting(LineItem::total)
                .containsExactly(money("899.99"), money("45.00"), money("12.00"));
    }

    @Test
    void appliesToTheCurrentTotalSoEarlierDiscountsCompound() {
        PricingContext context = context(item("Laptop", "ELECTRONICS", "100.00", 1));
        PercentOffCategoryRule extra =
                new PercentOffCategoryRule(UUID.randomUUID(), "Extra 20%", "ELECTRONICS", BigDecimal.valueOf(20));

        rule.apply(context);
        extra.apply(context);

        assertThat(context.currentTotal()).isEqualTo(money("72.00"));
    }

    @Test
    void roundsEachDiscountHalfUpToCents() {
        PricingContext context = context(item("Cable", "ELECTRONICS", "0.15", 1));

        rule.apply(context);

        assertThat(context.lines().getFirst().discounts().getFirst().amount()).isEqualTo(money("0.02"));
        assertThat(context.currentTotal()).isEqualTo(money("0.13"));
    }

    @Test
    void aHundredPercentDiscountMakesTheLineFree() {
        PricingContext context = context(item("Laptop", "ELECTRONICS", "10.00", 3));

        new PercentOffCategoryRule(UUID.randomUUID(), "Free", "ELECTRONICS", BigDecimal.valueOf(100)).apply(context);

        assertThat(context.currentTotal()).isEqualTo(money("0.00"));
    }

    @Test
    void declaresItsTypeAndStage() {
        assertThat(rule.type()).isEqualTo(PromotionType.PERCENT_OFF_CATEGORY);
        assertThat(rule.priority()).isEqualTo(RulePriority.CATEGORY_DISCOUNT);
    }

    @Test
    void providerBuildsTheRuleFromItsDefinition() {
        PercentOffCategoryPromotion promotion =
                new PercentOffCategoryPromotion("Sale", Set.of(), "books", new BigDecimal("12.50"));

        assertThat(new PercentOffCategoryRule.Provider().create(promotion))
                .isEqualTo(new PercentOffCategoryRule(promotion.getId(), "Sale", "BOOKS", new BigDecimal("12.50")));
    }
}
