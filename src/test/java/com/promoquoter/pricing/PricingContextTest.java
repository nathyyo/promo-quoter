package com.promoquoter.pricing;

import com.promoquoter.pricing.rules.PercentOffCategoryRule;
import com.promoquoter.promotion.CustomerSegment;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static com.promoquoter.pricing.PricingFixtures.context;
import static com.promoquoter.pricing.PricingFixtures.item;
import static com.promoquoter.pricing.PricingFixtures.money;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PricingContextTest {

    private final PromotionRule rule =
            new PercentOffCategoryRule(UUID.randomUUID(), "Test", "MISC", BigDecimal.TEN);

    @Test
    void lineSubtotalIsUnitPriceTimesQuantity() {
        LineItem line = context(item("Pen", "MISC", "1.25", 3)).lines().getFirst();

        assertThat(line.subtotal()).isEqualTo(money("3.75"));
        assertThat(line.total()).isEqualTo(money("3.75"));
    }

    @Test
    void discountsAreRoundedAndRecordedOnTheLine() {
        LineItem line = context(item("Pen", "MISC", "10.00", 1)).lines().getFirst();

        BigDecimal applied = line.applyDiscount(rule, money("1.005"));

        assertThat(applied).isEqualTo(money("1.01"));
        assertThat(line.total()).isEqualTo(money("8.99"));
        assertThat(line.discounts()).containsExactly(new LineDiscount(rule.promotionId(), "Test", money("1.01")));
    }

    @Test
    void aDiscountCanNeverMakeALineNegative() {
        LineItem line = context(item("Pen", "MISC", "5.00", 1)).lines().getFirst();

        assertThat(line.applyDiscount(rule, money("7.00"))).isEqualTo(money("5.00"));
        assertThat(line.applyDiscount(rule, money("1.00"))).isEqualTo(money("0.00"));
        assertThat(line.total()).isEqualTo(money("0.00"));
        assertThat(line.discounts()).hasSize(1);
    }

    @Test
    void zeroDiscountsAreNotRecorded() {
        LineItem line = context(item("Pen", "MISC", "5.00", 1)).lines().getFirst();

        line.applyDiscount(rule, money("0.004"));

        assertThat(line.discounts()).isEmpty();
    }

    @Test
    void rejectsNegativeDiscounts() {
        LineItem line = context(item("Pen", "MISC", "5.00", 1)).lines().getFirst();

        assertThatThrownBy(() -> line.applyDiscount(rule, money("-1.00")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void currentTotalSumsAllLines() {
        PricingContext context = context(item("A", "MISC", "1.10", 2), item("B", "MISC", "0.30", 1));

        assertThat(context.currentTotal()).isEqualTo(money("2.50"));
    }

    @Test
    void rejectsTwoLinesForTheSameProduct() {
        PricingItem first = item("A", "MISC", "1.00", 1);
        PricingItem duplicate = new PricingItem(first.productId(), "A", "MISC", money("1.00"), 2);

        assertThatThrownBy(() -> new PricingContext(CustomerSegment.REGULAR, List.of(first, duplicate)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(first.productId().toString());
    }

    @Test
    void pricingItemsRejectInvalidValues() {
        assertThatThrownBy(() -> item("A", "MISC", "-1.00", 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> item("A", "MISC", "1.00", 0)).isInstanceOf(IllegalArgumentException.class);
    }
}
