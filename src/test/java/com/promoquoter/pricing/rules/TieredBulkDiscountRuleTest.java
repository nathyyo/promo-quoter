package com.promoquoter.pricing.rules;

import com.promoquoter.pricing.PricingContext;
import com.promoquoter.pricing.PricingItem;
import com.promoquoter.pricing.RulePriority;
import com.promoquoter.pricing.rules.TieredBulkDiscountRule.Tier;
import com.promoquoter.promotion.DiscountTier;
import com.promoquoter.promotion.PromotionType;
import com.promoquoter.promotion.TieredBulkDiscountPromotion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.promoquoter.pricing.PricingFixtures.context;
import static com.promoquoter.pricing.PricingFixtures.item;
import static com.promoquoter.pricing.PricingFixtures.money;
import static org.assertj.core.api.Assertions.assertThat;

class TieredBulkDiscountRuleTest {

    @ParameterizedTest(name = "{0} units at 10.00 → {1}")
    @CsvSource({
            "1,  10.00",    // below the first tier
            "9,  90.00",
            "10, 95.00",    // exactly the first threshold: 5% off 100.00
            "19, 180.50",   // 5% off 190.00
            "20, 180.00",   // second tier: 10% off 200.00
            "50, 450.00"    // above the highest tier keeps the highest percentage
    })
    void appliesTheHighestTierReached(int quantity, String expectedTotal) {
        PricingItem pen = item("Pen", "OFFICE", "10.00", quantity);
        PricingContext context = context(pen);

        rule(pen.productId()).apply(context);

        assertThat(context.currentTotal()).isEqualTo(money(expectedTotal));
    }

    @Test
    void recordsASingleDiscountEvenWhenSeveralTiersAreReached() {
        PricingItem pen = item("Pen", "OFFICE", "10.00", 25);
        PricingContext context = context(pen);

        rule(pen.productId()).apply(context);

        assertThat(context.lines().get(0).discounts()).hasSize(1);
    }

    @Test
    void ignoresOtherProducts() {
        PricingContext context = context(item("Pen", "OFFICE", "10.00", 30));

        rule(UUID.randomUUID()).apply(context);

        assertThat(context.currentTotal()).isEqualTo(money("300.00"));
    }

    @Test
    void sortsTiersRegardlessOfInputOrder() {
        TieredBulkDiscountRule rule = new TieredBulkDiscountRule(UUID.randomUUID(), "Bulk", UUID.randomUUID(),
                List.of(new Tier(20, BigDecimal.TEN), new Tier(10, BigDecimal.valueOf(5))));

        assertThat(rule.tiers()).extracting(Tier::minQuantity).containsExactly(10, 20);
        assertThat(rule.tierFor(15)).map(Tier::percent).contains(BigDecimal.valueOf(5));
        assertThat(rule.tierFor(5)).isEmpty();
    }

    @Test
    void declaresItsTypeAndStage() {
        TieredBulkDiscountRule rule = rule(UUID.randomUUID());

        assertThat(rule.type()).isEqualTo(PromotionType.TIERED_BULK_DISCOUNT);
        assertThat(rule.priority()).isEqualTo(RulePriority.VOLUME_DISCOUNT);
    }

    @Test
    void providerBuildsTheRuleFromItsDefinition() {
        UUID productId = UUID.randomUUID();
        TieredBulkDiscountPromotion promotion = new TieredBulkDiscountPromotion("Bulk", Set.of(), productId,
                List.of(new DiscountTier(10, BigDecimal.valueOf(5))));

        assertThat(new TieredBulkDiscountRule.Provider().create(promotion)).isEqualTo(
                new TieredBulkDiscountRule(promotion.getId(), "Bulk", productId,
                        List.of(new Tier(10, BigDecimal.valueOf(5)))));
    }

    private static TieredBulkDiscountRule rule(UUID productId) {
        return new TieredBulkDiscountRule(UUID.randomUUID(), "Bulk", productId, List.of(
                new Tier(10, BigDecimal.valueOf(5)),
                new Tier(20, BigDecimal.TEN)));
    }
}
