package com.promoquoter.pricing;

import com.promoquoter.pricing.rules.BuyXGetYRule;
import com.promoquoter.pricing.rules.PercentOffCategoryRule;
import com.promoquoter.pricing.rules.TieredBulkDiscountRule;
import com.promoquoter.promotion.BuyXGetYPromotion;
import com.promoquoter.promotion.DiscountTier;
import com.promoquoter.promotion.PercentOffCategoryPromotion;
import com.promoquoter.promotion.Promotion;
import com.promoquoter.promotion.PromotionType;
import com.promoquoter.promotion.TieredBulkDiscountPromotion;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.promoquoter.pricing.PricingFixtures.defaultRuleFactory;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PromotionRuleFactoryTest {

    private final PromotionRuleFactory factory = defaultRuleFactory();

    @Test
    void createsTheMatchingRuleForEachPromotionType() {
        UUID productId = UUID.randomUUID();

        assertThat(factory.create(new PercentOffCategoryPromotion("A", Set.of(), "books", BigDecimal.TEN)))
                .isInstanceOf(PercentOffCategoryRule.class);
        assertThat(factory.create(new TieredBulkDiscountPromotion("B", Set.of(), productId,
                List.of(new DiscountTier(5, BigDecimal.ONE)))))
                .isInstanceOf(TieredBulkDiscountRule.class);
        assertThat(factory.create(new BuyXGetYPromotion("C", Set.of(), productId, 2, 1)))
                .isInstanceOf(BuyXGetYRule.class);
    }

    @Test
    void resolvesSubclassesOfASupportedDefinition() {
        PercentOffCategoryPromotion subclass =
                new PercentOffCategoryPromotion("Proxy-like", Set.of(), "books", BigDecimal.TEN) {
                };

        assertThat(factory.create(subclass)).isInstanceOf(PercentOffCategoryRule.class);
    }

    @Test
    void failsClearlyForAnUnsupportedDefinition() {
        assertThatThrownBy(() -> factory.create(new UnsupportedPromotion()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No rule provider registered");
    }

    @Test
    void refusesTwoProvidersForTheSameDefinition() {
        List<PromotionRuleProvider<?>> duplicates =
                List.of(new BuyXGetYRule.Provider(), new BuyXGetYRule.Provider());

        assertThatThrownBy(() -> new PromotionRuleFactory(duplicates))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("BuyXGetYPromotion");
    }

    private static final class UnsupportedPromotion extends Promotion {

        UnsupportedPromotion() {
            super("Unsupported", Set.of());
        }

        @Override
        public PromotionType getType() {
            return PromotionType.PERCENT_OFF_CATEGORY;
        }
    }
}
