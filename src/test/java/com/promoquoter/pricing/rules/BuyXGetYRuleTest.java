package com.promoquoter.pricing.rules;

import com.promoquoter.pricing.PricingContext;
import com.promoquoter.pricing.PricingItem;
import com.promoquoter.pricing.RulePriority;
import com.promoquoter.promotion.BuyXGetYPromotion;
import com.promoquoter.promotion.PromotionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import static com.promoquoter.pricing.PricingFixtures.context;
import static com.promoquoter.pricing.PricingFixtures.item;
import static com.promoquoter.pricing.PricingFixtures.money;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BuyXGetYRuleTest {

    @ParameterizedTest(name = "buy 2 get 1, {0} units → {1} free")
    @CsvSource({"1, 0", "2, 0", "3, 1", "5, 1", "6, 2", "7, 2", "9, 3"})
    void countsFreeUnitsPerCompleteGroup(int quantity, int expectedFree) {
        assertThat(rule(UUID.randomUUID(), 2, 1).freeUnits(quantity)).isEqualTo(expectedFree);
    }

    @Test
    void supportsMoreThanOneFreeUnitPerGroup() {
        BuyXGetYRule buyThreeGetTwo = rule(UUID.randomUUID(), 3, 2);

        assertThat(buyThreeGetTwo.freeUnits(4)).isZero();
        assertThat(buyThreeGetTwo.freeUnits(5)).isEqualTo(2);
        assertThat(buyThreeGetTwo.freeUnits(10)).isEqualTo(4);
    }

    @Test
    void takesTheFreeUnitsOffTheLine() {
        PricingItem laptop = item("Laptop", "ELECTRONICS", "100.00", 6);
        PricingContext context = context(laptop);

        rule(laptop.productId(), 2, 1).apply(context);

        assertThat(context.currentTotal()).isEqualTo(money("400.00"));
        assertThat(context.lines().getFirst().discounts()).singleElement()
                .satisfies(discount -> assertThat(discount.amount()).isEqualTo(money("200.00")));
    }

    @Test
    void valuesFreeUnitsAtTheAlreadyDiscountedPrice() {
        PricingItem laptop = item("Laptop", "ELECTRONICS", "100.00", 3);
        PricingContext context = context(laptop);
        new PercentOffCategoryRule(UUID.randomUUID(), "10% off", "ELECTRONICS", BigDecimal.TEN).apply(context);

        rule(laptop.productId(), 2, 1).apply(context);

        // 300.00 - 10% = 270.00; one of three units free = 90.00 off
        assertThat(context.currentTotal()).isEqualTo(money("180.00"));
    }

    @Test
    void roundsTheFreeUnitValueHalfUp() {
        PricingItem pen = item("Pen", "OFFICE", "0.99", 3);
        PricingContext context = context(pen);
        new PercentOffCategoryRule(UUID.randomUUID(), "15% off", "OFFICE", BigDecimal.valueOf(15)).apply(context);

        rule(pen.productId(), 2, 1).apply(context);

        // 2.97 - 0.45 = 2.52; 2.52 / 3 = 0.84 free
        assertThat(context.currentTotal()).isEqualTo(money("1.68"));
    }

    @Test
    void doesNothingWhenTheProductIsMissingOrTooFewUnits() {
        PricingItem laptop = item("Laptop", "ELECTRONICS", "100.00", 2);
        PricingContext context = context(laptop);

        rule(UUID.randomUUID(), 1, 1).apply(context);
        rule(laptop.productId(), 2, 1).apply(context);

        assertThat(context.currentTotal()).isEqualTo(money("200.00"));
        assertThat(context.lines().getFirst().discounts()).isEmpty();
    }

    @Test
    void rejectsNonPositiveQuantities() {
        assertThatThrownBy(() -> rule(UUID.randomUUID(), 0, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> rule(UUID.randomUUID(), 1, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void declaresItsTypeAndStage() {
        BuyXGetYRule rule = rule(UUID.randomUUID(), 2, 1);

        assertThat(rule.type()).isEqualTo(PromotionType.BUY_X_GET_Y);
        assertThat(rule.priority()).isEqualTo(RulePriority.FREE_ITEMS);
    }

    @Test
    void providerBuildsTheRuleFromItsDefinition() {
        UUID productId = UUID.randomUUID();
        BuyXGetYPromotion promotion = new BuyXGetYPromotion("B2G1", Set.of(), productId, 2, 1);

        assertThat(new BuyXGetYRule.Provider().create(promotion))
                .isEqualTo(new BuyXGetYRule(promotion.getId(), "B2G1", productId, 2, 1));
    }

    private static BuyXGetYRule rule(UUID productId, int buy, int free) {
        return new BuyXGetYRule(UUID.randomUUID(), "Buy " + buy + " get " + free, productId, buy, free);
    }
}
